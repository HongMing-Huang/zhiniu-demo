import UIKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        window = UIWindow(frame: UIScreen.main.bounds)
        // Default page; CLI preview overrides via URL scheme / env（KUIKLY_PAGE、KUIKLY_PAGE_DATA JSON）
        let pageName = ProcessInfo.processInfo.environment["KUIKLY_PAGE"] ?? "MarketList"
        var pageData: [String: Any] = [:]
        if let json = ProcessInfo.processInfo.environment["KUIKLY_PAGE_DATA"],
           let data = json.data(using: .utf8),
           let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            pageData = dict
        }
        let vc = KuiklyRenderViewController(pageName: pageName, pageData: pageData)
        let nav = UINavigationController(rootViewController: vc)
        nav.isNavigationBarHidden = true
        window?.rootViewController = nav
        window?.makeKeyAndVisible()
        return true
    }
}
