import XCTest
import UIKit
import WebKit
@testable import StickerPort

final class AppTests: XCTestCase {
 func testNetworkPolicy() {
  XCTAssertTrue(MediaDownload.allowed(URL(string: "https://a.ibyteimg.com/x")!))
  for text in ["http://a.ibyteimg.com/x", "https://ibyteimg.com.evil.test/x", "https://127.0.0.1/x", "https://user@a.ibyteimg.com/x"] { XCTAssertFalse(MediaDownload.allowed(URL(string: text)!)) }
  XCTAssertTrue(BrowserController.tiktok(URL(string: "https://vm.tiktok.com/test")!)); XCTAssertFalse(BrowserController.tiktok(URL(string: "https://tiktok.com.evil.test")!))
 }
 func testStorageRoundTrip() throws { let original = try Storage.load(); defer { try? Storage.save(original) }; try Storage.save(["packs": [["name": "Prova italiana"]]]); let result = try Storage.load()["packs"] as! [[String: Any]]; XCTAssertEqual(result[0]["name"] as? String, "Prova italiana") }
 @MainActor func testSharedInterfaceAndBridgeLoad() async throws {
  let original = try Storage.load(); try Storage.save(["packs": []]); defer { try? Storage.save(original) }
  let controller = MainController(); controller.loadViewIfNeeded(); let window = UIWindow(frame: UIScreen.main.bounds); window.rootViewController = controller; window.makeKeyAndVisible(); defer { window.isHidden = true }
  var loaded = false
  for _ in 0..<60 { try await Task.sleep(nanoseconds: 250_000_000); if let text = try? await controller.home.evaluateJavaScript("typeof window.stickerCandidates + ':' + document.getElementById('count').textContent") as? String, text == "function:0" { loaded = true; break } }
  let diagnostic = try? await controller.home.evaluateJavaScript("JSON.stringify({ready:document.readyState,module:typeof window.stickerCandidates,status:document.getElementById('status')?.textContent})")
  XCTAssertTrue(loaded, "Shared UI and native storage must load: \(String(describing: diagnostic))")
  guard loaded else { return }
  _ = try await controller.home.evaluateJavaScript("document.getElementById('new').click(); document.getElementById('dialogInput').value='Prova completa'; document.getElementById('confirm').click(); true")
  var created = false
  for _ in 0..<40 { try await Task.sleep(nanoseconds: 100_000_000); if let text = try? await controller.home.evaluateJavaScript("document.getElementById('count').textContent") as? String, text == "1" { created = true; break } }
  XCTAssertTrue(created); guard created else { return }
  let importScript = """
  (()=>{const transfer=new DataTransfer();for(const color of ['red','green','blue']){const c=document.createElement('canvas');c.width=c.height=160;const x=c.getContext('2d');x.fillStyle=color;x.fillRect(20,20,120,120);const bytes=Uint8Array.from(atob(c.toDataURL('image/png').split(',')[1]),v=>v.charCodeAt(0));transfer.items.add(new File([bytes],color+'.png',{type:'image/png'}));}const input=document.getElementById('file');input.files=transfer.files;input.dispatchEvent(new Event('change'));return true;})()
  """
  _ = try await controller.home.evaluateJavaScript(importScript)
  var imported = false
  for _ in 0..<80 { try await Task.sleep(nanoseconds: 200_000_000); if let count = try? await controller.home.evaluateJavaScript("document.getElementById('stickerGrid').children.length") as? Int, count == 3 { imported = true; break } }
  let status = try? await controller.home.evaluateJavaScript("document.getElementById('status').textContent")
  XCTAssertTrue(imported, "Import three PNGs through the real UI and native WebP encoder: \(String(describing: status))")
  let packs = try Storage.load()["packs"] as! [[String: Any]]; XCTAssertEqual((packs[0]["stickers"] as? [Any])?.count, 3)
 }
 func testPackProtocolAndMinimumCount() throws {
  let format = UIGraphicsImageRendererFormat(); format.scale = 1
  let renderer = UIGraphicsImageRenderer(size: CGSize(width: 96, height: 96), format: format); let tray = renderer.pngData { context in UIColor.green.setFill(); context.fill(CGRect(x: 0, y: 0, width: 96, height: 96)) }.base64EncodedString()
  let image = UIGraphicsImageRenderer(size: CGSize(width: 512, height: 512), format: format).pngData { context in UIColor.green.setFill(); context.fill(CGRect(x: 0, y: 0, width: 512, height: 512)) }.base64EncodedString()
  let sticker: [String: Any] = ["animated": false, "data": image]; let p: [String: Any] = ["name": "Reazioni", "animated": false, "stickers": [sticker,sticker,sticker]]
  let data = try Storage.packPayload(p, tray: tray); let json = try JSONSerialization.jsonObject(with: data) as! [String: Any]
  XCTAssertEqual(json["publisher"] as? String, "StickerPort"); XCTAssertEqual((json["stickers"] as? [Any])?.count, 3); XCTAssertNotNil(json["tray_image"])
  let first = (json["stickers"] as! [[String: Any]])[0]; let bytes = Data(base64Encoded: first["image_data"] as! String)!
  XCTAssertEqual(String(data: bytes.prefix(4), encoding: .ascii), "RIFF"); XCTAssertEqual(String(data: bytes[8..<12], encoding: .ascii), "WEBP")
  XCTAssertThrowsError(try Storage.packPayload(["name": "Reazioni", "animated": false, "stickers": [sticker]], tray: tray))
 }
}
