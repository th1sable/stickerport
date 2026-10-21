import UIKit
import WebKit
import ImageIO

@main final class AppDelegate: UIResponder, UIApplicationDelegate {
 var window: UIWindow?
 func application(_ application: UIApplication, didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
  window = UIWindow(frame: UIScreen.main.bounds); window?.rootViewController = MainController(); window?.makeKeyAndVisible(); return true
 }
}

enum AppError: LocalizedError {
 case message(String)
 var errorDescription: String? { if case let .message(text) = self { return text }; return nil }
}

enum Storage {
 static var directory: URL { FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("StickerPort") }
 static var file: URL { directory.appendingPathComponent("library.json") }
 static func load() throws -> [String: Any] {
  guard FileManager.default.fileExists(atPath: file.path) else { return ["packs": []] }
  guard let value = try JSONSerialization.jsonObject(with: Data(contentsOf: file)) as? [String: Any], value["packs"] is [Any] else { throw AppError.message("Raccolta non leggibile.") }; return value
 }
 static func save(_ value: [String: Any]) throws {
  guard value["packs"] is [Any] else { throw AppError.message("Raccolta non valida.") }
  let data = try JSONSerialization.data(withJSONObject: value)
  guard data.count <= 100 * 1024 * 1024 else { throw AppError.message("Raccolta troppo grande. Elimina alcuni pacchetti.") }
  try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
  try data.write(to: file, options: [.atomic, .completeFileProtectionUnlessOpen])
  var url = directory; var values = URLResourceValues(); values.isExcludedFromBackup = true; try url.setResourceValues(values)
 }
 static func packPayload(_ pack: [String: Any], tray: String) throws -> Data {
  guard let name = pack["name"] as? String, !name.isEmpty, name.count <= 64,
        let animated = pack["animated"] as? Bool, let stickers = pack["stickers"] as? [[String: Any]], (3...30).contains(stickers.count),
        let icon = Data(base64Encoded: tray), icon.count <= 50 * 1024, let image = UIImage(data: icon), image.size == CGSize(width: 96, height: 96) else { throw AppError.message("Pacchetto non valido.") }
  var items = [[String: Any]]()
  for s in stickers {
   guard let isAnimated = s["animated"] as? Bool, isAnimated == animated, let encoded = s["data"] as? String, let data = Data(base64Encoded: encoded), !data.isEmpty, data.count <= (animated ? 500 : 100) * 1024 else { throw AppError.message("Sticker non compatibile.") }
   guard let source = CGImageSourceCreateWithData(data as CFData, nil), let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any], properties[kCGImagePropertyPixelWidth] as? Int == 512, properties[kCGImagePropertyPixelHeight] as? Int == 512 else { throw AppError.message("Lo sticker deve misurare 512 × 512 pixel.") }
   items.append(["image_data": encoded, "emojis": [], "accessibility_text": "Sticker"])
  }
  return try JSONSerialization.data(withJSONObject: ["identifier": "sp-" + UUID().uuidString, "name": name, "publisher": "StickerPort", "tray_image": tray, "animated_sticker_pack": animated, "stickers": items])
 }
}

final class MediaDownload: NSObject, URLSessionDataDelegate {
 var session: URLSession!; var data = Data(); var completion: ((Result<Data, Error>) -> Void)?
 static func allowed(_ url: URL) -> Bool {
  guard url.scheme == "https", url.user == nil, url.password == nil, url.port == nil || url.port == 443, let host = url.host?.lowercased() else { return false }
  return ["ibyteimg.com", "tiktokcdn.com", "tiktokcdn-us.com", "byteoversea.com"].contains { host == $0 || host.hasSuffix("." + $0) }
 }
 func start(_ url: URL, completion: @escaping (Result<Data, Error>) -> Void) {
  guard Self.allowed(url) else { completion(.failure(AppError.message("Origine immagine non supportata."))); return }
  self.completion = completion; let configuration = URLSessionConfiguration.ephemeral; configuration.timeoutIntervalForRequest = 20; configuration.timeoutIntervalForResource = 45
  session = URLSession(configuration: configuration, delegate: self, delegateQueue: nil); session.dataTask(with: url).resume()
 }
 func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(request.url.map(Self.allowed) == true ? request : nil) }
 func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive response: URLResponse, completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
  guard let http = response as? HTTPURLResponse, http.statusCode == 200, response.expectedContentLength <= 8 * 1024 * 1024 else { completionHandler(.cancel); return }; completionHandler(.allow)
 }
 func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive chunk: Data) { if data.count + chunk.count > 8 * 1024 * 1024 { dataTask.cancel() } else { data.append(chunk) } }
 func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) { let callback = completion; completion = nil; if let error = error { callback?(.failure(error)) } else { callback?(.success(data)) }; session.finishTasksAndInvalidate() }
}

final class MainController: UIViewController, WKScriptMessageHandler, WKNavigationDelegate {
 var home: WKWebView!; var downloads = [Int: MediaDownload]()
 override func viewDidLoad() {
  super.viewDidLoad(); view.backgroundColor = UIColor(red: 16/255, green: 17/255, blue: 24/255, alpha: 1)
  let config = WKWebViewConfiguration(); config.userContentController.add(self, name: "native"); config.websiteDataStore = .nonPersistent()
  home = WKWebView(frame: .zero, configuration: config); home.navigationDelegate = self; home.isOpaque = false; home.backgroundColor = view.backgroundColor; home.scrollView.backgroundColor = view.backgroundColor; home.translatesAutoresizingMaskIntoConstraints = false
  view.addSubview(home); NSLayoutConstraint.activate([home.leadingAnchor.constraint(equalTo: view.leadingAnchor), home.trailingAnchor.constraint(equalTo: view.trailingAnchor), home.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor), home.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor)])
  if let url = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "web") { home.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent()) }
 }
 func reply(_ id: Int, result: Any? = nil, error: String? = nil) {
  DispatchQueue.main.async { let values: [Any] = [id, result ?? NSNull(), error as Any? ?? NSNull()]; guard let data = try? JSONSerialization.data(withJSONObject: values), let json = String(data: data, encoding: .utf8) else { return }; self.home.evaluateJavaScript("window.nativeReply(...\(json))", completionHandler: nil) }
 }
 func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
  guard message.frameInfo.isMainFrame, message.webView === home, let body = message.body as? [String: Any], let id = body["id"] as? Int, let action = body["action"] as? String, let data = body["data"] as? [String: Any] else { return }
  do { switch action {
   case "load": reply(id, result: try Storage.load())
   case "save": guard let library = data["library"] as? [String: Any] else { throw AppError.message("Raccolta non valida.") }; try Storage.save(library); reply(id, result: true)
   case "download": guard let text = data["url"] as? String, let url = URL(string: text) else { throw AppError.message("Link non valido.") }; let download = MediaDownload(); downloads[id] = download; download.start(url) { result in DispatchQueue.main.async { self.downloads.removeValue(forKey: id); switch result { case .success(let bytes): self.reply(id, result: bytes.base64EncodedString()); case .failure: self.reply(id, error: "Sticker non disponibile. Riapri TikTok e raccoglilo di nuovo.") } } }
   case "browse": guard let text = data["url"] as? String, let url = URL(string: text), BrowserController.tiktok(url), let scanner = data["scanner"] as? String else { throw AppError.message("Link TikTok non valido.") }
    let browser = BrowserController(url: url, scanner: scanner); browser.onCollect = { items in guard let jsonData = try? JSONSerialization.data(withJSONObject: items), let json = String(data: jsonData, encoding: .utf8) else { return }; self.home.evaluateJavaScript("window.stickerCandidates(\(json))", completionHandler: nil) }; let navigation = UINavigationController(rootViewController: browser); navigation.modalPresentationStyle = .fullScreen; present(navigation, animated: true); reply(id, result: true)
   case "export": guard let pack = data["pack"] as? [String: Any], let tray = data["tray"] as? String else { throw AppError.message("Pacchetto non valido.") }; let payload = try Storage.packPayload(pack, tray: tray); guard UIApplication.shared.canOpenURL(URL(string: "whatsapp://")!) else { throw AppError.message("Installa WhatsApp prima di aggiungere il pacchetto.") }
    UIPasteboard.general.setItems([["net.whatsapp.third-party.sticker-pack": payload]], options: [.localOnly: true, .expirationDate: Date().addingTimeInterval(60)])
    UIApplication.shared.open(URL(string: "whatsapp://stickerPack")!, options: [:]) { opened in self.reply(id, result: opened, error: opened ? nil : "Impossibile aprire WhatsApp.") }
   default: throw AppError.message("Comando sconosciuto.")
  }} catch { reply(id, error: error.localizedDescription) }
 }
 func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) { decisionHandler(navigationAction.request.url?.isFileURL == true ? .allow : .cancel) }
}

final class BrowserController: UIViewController, WKNavigationDelegate {
 let url: URL; let scanner: String; var browser: WKWebView!; var onCollect: (([Any]) -> Void)?
 init(url: URL, scanner: String) { self.url = url; self.scanner = scanner; super.init(nibName: nil, bundle: nil) }
 required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
 static func tiktok(_ url: URL) -> Bool { guard url.scheme == "https", url.user == nil, url.password == nil, url.port == nil || url.port == 443, let host = url.host else { return false }; return host == "tiktok.com" || host.hasSuffix(".tiktok.com") }
 override func viewDidLoad() {
  super.viewDidLoad(); title = "TikTok"; view.backgroundColor = .black
  navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Raccolta", style: .plain, target: self, action: #selector(close))
  navigationItem.rightBarButtonItem = UIBarButtonItem(title: "Raccogli sticker", style: .done, target: self, action: #selector(collect))
  let config = WKWebViewConfiguration(); config.defaultWebpagePreferences.preferredContentMode = .desktop; config.allowsInlineMediaPlayback = true
  browser = WKWebView(frame: view.bounds, configuration: config); browser.autoresizingMask = [.flexibleWidth, .flexibleHeight]; browser.navigationDelegate = self; browser.allowsBackForwardNavigationGestures = true; view.addSubview(browser); browser.load(URLRequest(url: url))
 }
 @objc func close() { dismiss(animated: true) }
 @objc func collect() { guard let current = browser.url, Self.tiktok(current) else { alert("Apri una pagina TikTok."); return }; browser.evaluateJavaScript(scanner) { result, error in guard error == nil, let items = result as? [Any] else { self.alert("Pagina non pronta. Riprova."); return }; self.onCollect?(items); self.dismiss(animated: true) } }
 func alert(_ text: String) { let a = UIAlertController(title: "StickerPort", message: text, preferredStyle: .alert); a.addAction(UIAlertAction(title: "OK", style: .default)); present(a, animated: true) }
 func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) { decisionHandler(navigationAction.request.url?.scheme == "https" ? .allow : .cancel) }
}
