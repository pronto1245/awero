import Foundation

@MainActor
final class AlarmCoordinator {
    let store: AlarmStore
    let scheduler: AlarmScheduler

    init(store: AlarmStore, scheduler: AlarmScheduler = AlarmScheduler()) {
        self.store = store
        self.scheduler = scheduler
    }

    func create(_ alarm: Alarm) async {
        store.save(alarm)
        try? await scheduler.schedule(alarm)
    }

    func update(_ alarm: Alarm) async {
        var next = alarm
        next.version += 1
        store.save(next)
        try? await scheduler.schedule(next)
    }

    func delete(_ alarm: Alarm) async {
        await scheduler.cancel(alarm)
        store.delete(alarm)
    }

    func test(_ alarm: Alarm) async {
        try? await scheduler.scheduleTest(for: alarm)
    }

    func repairAll() async {
        await AlarmRecovery(scheduler: scheduler, store: store).reconcile()
    }
}
