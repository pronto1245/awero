import UIKit
import UserNotifications

@MainActor
final class AWEROAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private let wakeFlow = WakeFlowController.shared

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        startWakeIfValid(response.notification.request.identifier)
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        startWakeIfValid(notification.request.identifier)
        completionHandler([.banner, .sound, .badge])
    }

    private func startWakeIfValid(_ identifier: String) {
        let parts = identifier.split(separator: ":")
        guard parts.count >= 5,
              parts[0] == "awero",
              parts[1] == "alarm",
              let id = UUID(uuidString: String(parts[2])),
              let version = Int(parts[3].replacingOccurrences(of: "v", with: "")) else {
            return
        }

        guard let data = UserDefaults.standard.data(forKey: "awero.alarms.v1"),
              let alarms = try? JSONDecoder().decode([Alarm].self, from: data),
              let alarm = alarms.first(where: { $0.id == id && $0.version == version && $0.enabled }) else {
            return
        }

        Task { @MainActor in
            wakeFlow.start(alarm: alarm, scheduledAt: Date())
        }
    }
}
