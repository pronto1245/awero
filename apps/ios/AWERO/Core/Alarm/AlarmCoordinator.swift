import Foundation

enum AlarmCoordinatorError: LocalizedError {
    case persistenceFailed

    var errorDescription: String? {
        String(localized: "alarm.error.persistence")
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
            await enqueueSync("CREATE_ALARM", alarm: alarm, clientVersion: alarm.version)
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
            await enqueueSync("UPDATE_ALARM", alarm: next, clientVersion: alarm.version)
        } catch {
            _ = await store.save(alarm)
            try? await scheduler.schedule(alarm)
            throw error
        }
    }

    func delete(_ alarm: Alarm) async {
        guard await store.delete(alarm) else { return }
        await scheduler.cancel(alarm)
        await enqueueSync("DELETE_ALARM", alarm: alarm, clientVersion: alarm.version, payload: [:])
    }

    func test(_ alarm: Alarm) async throws {
        try await scheduler.scheduleTest(for: alarm)
    }

    private func enqueueSync(
        _ operationType: String,
        alarm: Alarm,
        clientVersion: Int,
        payload: [String: SyncJSONValue]? = nil
    ) async {
        let operation = SyncOperation(
            operationType: operationType,
            entityType: "ALARM",
            entityId: alarm.id.uuidString,
            clientVersion: clientVersion,
            payload: payload ?? [
                "label": .string("Alarm"),
                "hour": .integer(Int64(alarm.hour)),
                "minute": .integer(Int64(alarm.minute)),
                "enabled": .boolean(alarm.enabled),
                "weekdays": .array(alarm.weekdays.sorted().map { .integer(Int64($0)) }),
                "timezoneMode": .string(alarm.timezoneMode.rawValue),
                "fixedTimezone": alarm.fixedTimezone.map(SyncJSONValue.string) ?? .null,
                "snoozeEnabled": .boolean(true),
                "maxSnoozes": .integer(Int64(alarm.maxSnoozes)),
                "snoozeMinutes": .integer(Int64(alarm.snoozeMinutes)),
                "missionType": .string(alarm.missionType.rawValue),
                "difficulty": .string(alarm.difficulty.rawValue),
                "qrExpectedCode": alarm.qrExpectedCode.map(SyncJSONValue.string) ?? .null
            ]
        )
        await SyncQueueStore.shared.enqueue(operation)
        Task { await OfflineSyncCoordinator.shared.runOnce() }
    }

    func repairAll(forceReschedule: Bool = false) async {
        await store.load()
        await AlarmRecovery(scheduler: scheduler, store: store).reconcile(forceReschedule: forceReschedule)
    }
}
