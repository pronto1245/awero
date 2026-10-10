import Foundation
@MainActor
final class AlarmRecovery {
    private let scheduler: AlarmScheduler
    private let store: AlarmStore

    init(scheduler: AlarmScheduler, store: AlarmStore) {
        self.scheduler = scheduler
        self.store = store
    }

    func reconcile(forceReschedule: Bool = false) async {
        for alarm in store.alarms {
            guard alarm.enabled else {
                await scheduler.cancel(alarm)
                continue
            }

            let isScheduled = await scheduler.isScheduled(alarm)
            guard forceReschedule || !isScheduled else { continue }
            try? await scheduler.repair(alarm)
        }
    }
}
