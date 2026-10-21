import XCTest
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
  let controller = MainController(); controller.loadViewIfNeeded(); let window = UIWindow(frame: UIScreen.main.bounds); window.rootViewController = controller; window.makeKeyAndVisible(); defer { window.isHidden = true }
  var loaded = false
  for _ in 0..<60 { try await Task.sleep(nanoseconds: 250_000_000); if let text = try? await controller.home.evaluateJavaScript("typeof window.stickerCandidates + ':' + document.getElementById('count').textContent") as? String, text == "function:0" { loaded = true; break } }
  XCTAssertTrue(loaded, "Shared UI module and native storage must load in WKWebView")
 }
 func testPackProtocolAndMinimumCount() throws {
  let renderer = UIGraphicsImageRenderer(size: CGSize(width: 96, height: 96)); let tray = renderer.pngData { context in UIColor.green.setFill(); context.fill(CGRect(x: 0, y: 0, width: 96, height: 96)) }.base64EncodedString()
  let sticker: [String: Any] = ["animated": false, "data": tray]; let p: [String: Any] = ["name": "Reazioni", "animated": false, "stickers": [sticker,sticker,sticker]]
  let data = try Storage.packPayload(p, tray: tray); let json = try JSONSerialization.jsonObject(with: data) as! [String: Any]
  XCTAssertEqual(json["publisher"] as? String, "StickerPort"); XCTAssertEqual((json["stickers"] as? [Any])?.count, 3); XCTAssertNotNil(json["tray_image"])
  XCTAssertThrowsError(try Storage.packPayload(["name": "Reazioni", "animated": false, "stickers": [sticker]], tray: tray))
 }
}
