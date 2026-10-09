import UIKit
import UserNotifications

@MainActor
final class AWEROAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private let wakeFlow = WakeFlowController.shared
    private let database = CoreDataStore.shared

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
        handleNotification(response.notification.request.identifier)
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        handleNotification(notification.request.identifier)
        completionHandler([.banner, .sound, .badge])
    }

    private func handleNotification(_ identifier: String) {
        let parts = identifier.split(separator: ":")
        guard parts.count >= 3,
              parts[0] == "awero" else {
            return
        }

        if parts[1] == "test" || parts[1] == "snooze" {
            guard let id = UUID(uuidString: String(parts[2])) else { return }
            let isTestAlarm = parts[1] == "test" || parts.contains("test")
            Task { @MainActor in
                guard let alarm = await database.fetchAlarm(id: id), alarm.enabled else { return }
                await wakeFlow.start(alarm: alarm, scheduledAt: .now, isTestAlarm: isTestAlarm)
            }
            return
        }

        guard parts.count >= 5,
              parts[1] == "alarm",
              let id = UUID(uuidString: String(parts[2])),
              let version = Int(parts[3].replacingOccurrences(of: "v", with: "")) else {
            return
        }

        Task { @MainActor in
            guard let alarm = await database.fetchAlarm(id: id),
                  alarm.version == version,
                  alarm.enabled else { return }
            await wakeFlow.start(alarm: alarm, scheduledAt: .now)
        }
    }
}
