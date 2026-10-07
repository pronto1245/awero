import Foundation
import UserNotifications

@MainActor
final class AlarmRuntime: NSObject, UNUserNotificationCenterDelegate {
    private let scheduler: AlarmScheduler
    private let store: AlarmStore
    let wakeFlow = WakeFlowController()

    init(scheduler: AlarmScheduler, store: AlarmStore) {
        self.scheduler = scheduler
        self.store = store
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let parts = response.notification.request.identifier.split(separator: ":")
        if parts.count >= 5,
           parts[0] == "awero",
           parts[1] == "alarm",
           let alarmID = UUID(uuidString: String(parts[2])) {
            if let alarm = store.alarms.first(where: { $0.id == alarmID }),
               let scheduled = response.notification.request.trigger.flatMap({ _ in Date() }) as Date? {
                wakeFlow.begin(alarm: alarm, scheduledAt: scheduled)
            }
        }
        completionHandler()
    }
}
