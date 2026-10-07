import Foundation
import UserNotifications

final class AlarmScheduler {
    private let center = UNUserNotificationCenter.current()

    func schedule(_ alarm: Alarm, fireDate: DateComponents) async throws {
        let content = UNMutableNotificationContent()
        content.title = "AWERO"
        content.body = "Wake up. Stay up."
        content.sound = .default

        let trigger = UNCalendarNotificationTrigger(
            dateMatching: fireDate,
            repeats: false
        )

        let request = UNNotificationRequest(
            identifier: alarm.id.uuidString + ":" + String(alarm.version),
            content: content,
            trigger: trigger
        )

        try await center.add(request)
    }

    func cancel(_ alarm: Alarm) async {
        let prefix = alarm.id.uuidString + ":"
        let requests = await center.pendingNotificationRequests()
        let ids = requests.map(\.identifier).filter { $0.hasPrefix(prefix) }
        center.removePendingNotificationRequests(withIdentifiers: ids)
    }
}
