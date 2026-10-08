import Foundation

enum AlarmCoordinatorError: LocalizedError {
    case persistenceFailed

    var errorDescription: String? {
        "AWERO could not save the alarm. Check available storage and try again."
    }
}

@MainActor
final class AlarmCoordinator {
    let store: AlarmStore
    let scheduler: AlarmScheduler

    init(store: AlarmStore, scheduler: AlarmScheduler = AlarmScheduler()) {
        self.store = store
        self.scheduler = scheduler
    }

    func create(_ alarm: Alarm) async throws {
        guard await store.save(alarm) else {
            throw AlarmCoordinatorError.persistenceFailed
        }

        do {
            try await scheduler.schedule(alarm)
        } catch {
            await scheduler.cancel(alarm)
            _ = await store.delete(alarm)
            throw error
        }
    }

    func update(_ alarm: Alarm) async throws {
        var next = alarm
        next.version += 1
        guard await store.save(next) else {
            throw AlarmCoordinatorError.persistenceFailed
        }

        do {
            try await scheduler.schedule(next)
        } catch {
            _ = await store.save(alarm)
            try? await scheduler.schedule(alarm)
            throw error
        }
    }

    func delete(_ alarm: Alarm) async {
        guard await store.delete(alarm) else { return }
        await scheduler.cancel(alarm)
    }

    func test(_ alarm: Alarm) async throws {
        try await scheduler.scheduleTest(for: alarm)
    }

    func repairAll() async {
        await store.load()
        await AlarmRecovery(scheduler: scheduler, store: store).reconcile()
    }
}
