import Foundation
import UserNotifications

@MainActor
final class AlarmRecovery {
    private let scheduler: AlarmScheduler
    private let store: AlarmStore

    init(scheduler: AlarmScheduler, store: AlarmStore) {
        self.scheduler = scheduler
        self.store = store
    }

    func reconcile() async {
        try? await scheduler.requestAuthorization()
        for alarm in store.alarms {
            if alarm.enabled && !alarm.weekdays.isEmpty {
                if !(await scheduler.isScheduled(alarm)) {
                    try? await scheduler.repair(alarm)
                }
            } else {
                await scheduler.cancel(alarm)
            }
        }
    }
}
