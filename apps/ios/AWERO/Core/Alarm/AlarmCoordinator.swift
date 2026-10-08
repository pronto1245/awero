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
        guard await store.save(alarm) else { return }
        try? await scheduler.schedule(alarm)
    }

    func update(_ alarm: Alarm) async {
        var next = alarm
        next.version += 1
        guard await store.save(next) else { return }
        try? await scheduler.schedule(next)
    }

    func delete(_ alarm: Alarm) async {
        guard await store.delete(alarm) else { return }
        await scheduler.cancel(alarm)
    }

    func test(_ alarm: Alarm) async {
        try? await scheduler.scheduleTest(for: alarm)
    }

    func repairAll() async {
        await store.load()
        await AlarmRecovery(scheduler: scheduler, store: store).reconcile()
    }
}
