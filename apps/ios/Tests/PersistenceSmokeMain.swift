import Foundation
import CoreData
import UserNotifications

@main
struct PersistenceSmokeMain {
    static func main() async throws {
        if let phase = ProcessInfo.processInfo.environment["AWERO_CRASH_PHASE"] {
            try await runCrashPhase(phase)
            return
        }
        let root = FileManager.default.temporaryDirectory.appendingPathComponent("awero-smoke-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        let storeURL = root.appendingPathComponent("AWERO.sqlite")

        let oldModel = CoreDataStore.makeModelForTesting()
        oldModel.entities = oldModel.entities.filter {
            ["AlarmRecord", "StatisticsRecord", "WakeSessionRecord"].contains($0.name)
        }
        let oldContainer = NSPersistentContainer(name: "AWERO", managedObjectModel: oldModel)
        let oldDescription = NSPersistentStoreDescription(url: storeURL)
        oldDescription.shouldMigrateStoreAutomatically = false
        oldDescription.shouldInferMappingModelAutomatically = false
        oldContainer.persistentStoreDescriptions = [oldDescription]
        try await load(oldContainer)

        let alarmId = UUID()
        let object = NSEntityDescription.insertNewObject(forEntityName: "AlarmRecord", into: oldContainer.viewContext)
        object.setValue(alarmId.uuidString, forKey: "id")
        object.setValue(1, forKey: "version")
        object.setValue(7, forKey: "hour")
        object.setValue(30, forKey: "minute")
        object.setValue(true, forKey: "enabled")
        object.setValue("1,2,3,4,5", forKey: "weekdays")
        object.setValue(AlarmTimezoneMode.deviceLocal.rawValue, forKey: "timezoneMode")
        object.setValue(nil, forKey: "fixedTimezone")
        object.setValue(MissionType.math.rawValue, forKey: "missionType")
        object.setValue(Difficulty.medium.rawValue, forKey: "difficulty")
        object.setValue(3, forKey: "maxSnoozes")
        object.setValue(10, forKey: "snoozeMinutes")
        object.setValue(nil, forKey: "qrExpectedCode")
        try oldContainer.viewContext.save()

        let restarted = CoreDataStore(storeURL: storeURL)
        let restoredAlarm = await restarted.fetchAlarm(id: alarmId)
        precondition(restoredAlarm?.hour == 7 && restoredAlarm?.minute == 30)

        let readOnlyStore = CoreDataStore(storeURL: storeURL, readOnly: true)
        let saveFailure = await readOnlyStore.saveAlarm(
            Alarm(
                id: alarmId,
                version: 2,
                hour: 8,
                minute: 0,
                enabled: true,
                weekdays: Set(1...7),
                timezoneMode: .deviceLocal,
                fixedTimezone: nil,
                missionType: .math,
                difficulty: .medium
            )
        )
        precondition(!saveFailure)
        let unchangedAlarm = await restarted.fetchAlarm(id: alarmId)
        precondition(unchangedAlarm?.version == 1 && unchangedAlarm?.hour == 7)
        let failedManager = await MainActor.run { WakeSessionManager(database: readOnlyStore) }
        let failedTrigger = await failedManager.trigger(alarm: restoredAlarm!, scheduledAt: .now)
        precondition(!failedTrigger)
        let failedCurrent = await MainActor.run { failedManager.current }
        precondition(failedCurrent == nil)
        let failedFlow = await MainActor.run {
            WakeFlowController(sessionManager: failedManager, database: readOnlyStore)
        }
        await failedFlow.start(alarm: restoredAlarm!)
        let failedStartState = await MainActor.run { failedFlow.state }
        precondition(failedStartState == .idle)

        let operation = SyncOperation(
            operationType: "UPDATE_ALARM",
            entityType: "ALARM",
            entityId: alarmId.uuidString,
            clientVersion: 1,
            payload: ["hour": "8"]
        )
        await restarted.saveSyncOperation(operation)
        await restarted.saveSyncOperation(operation)
        let pending = await restarted.fetchDueSyncOperations()
        precondition(pending.count == 1)
        await restarted.retrySyncOperation(operation.id, nextAttemptAt: Date(timeIntervalSinceNow: 3600))
        let retryBlocked = await restarted.fetchDueSyncOperations()
        precondition(retryBlocked.isEmpty)
        await restarted.retrySyncOperation(operation.id, nextAttemptAt: .distantPast)
        let retryDue = await restarted.fetchDueSyncOperations()
        precondition(retryDue.count == 1)

        let session = WakeSession(
            id: UUID(),
            alarmId: alarmId,
            alarmVersion: 1,
            scheduledAt: .now,
            triggeredAt: .now,
            missionStartedAt: nil,
            completedAt: nil,
            result: nil,
            missionType: .math,
            completionTimeSeconds: nil,
            snoozeCount: 0,
            fallbackUsed: false,
            emergencyStop: false
        )
        let sessionSaved = await restarted.saveWakeSession(session)
        precondition(sessionSaved)
        _ = await failedManager.restore()
        await failedManager.startMission()
        await failedManager.setSnoozeCount(2)
        await failedManager.markFallback()
        let failedCompletion = await failedManager.complete()
        let failedEmergency = await failedManager.emergencyStop()
        precondition(failedCompletion == nil && failedEmergency == nil)
        let unchangedSession = await MainActor.run { failedManager.current }
        precondition(unchangedSession?.id == session.id)
        precondition(unchangedSession?.missionStartedAt == nil)
        precondition(unchangedSession?.snoozeCount == 0)
        precondition(unchangedSession?.fallbackUsed == false)
        precondition(unchangedSession?.result == nil && unchangedSession?.completedAt == nil)
        precondition(unchangedSession?.emergencyStop == false)
        await failedFlow.restore()
        await failedFlow.beginMission()
        await failedFlow.snooze()
        await failedFlow.emergencyStop()
        let failedFlowState = await MainActor.run { failedFlow.state }
        let failedSnoozes = await MainActor.run { failedFlow.snoozeCount }
        precondition(failedFlowState == .ringing && failedSnoozes == 0)
        let failedStatistics = await MainActor.run { StatisticsStore(database: readOnlyStore) }
        await failedStatistics.recordPlanned()
        var statisticsSession = session
        statisticsSession.result = "COMPLETED"
        statisticsSession.snoozeCount = 2
        statisticsSession.fallbackUsed = true
        statisticsSession.completionTimeSeconds = 10
        await failedStatistics.record(statisticsSession)
        let unchangedStatistics = await MainActor.run { failedStatistics.statistics }
        precondition(unchangedStatistics.planned == 0 && unchangedStatistics.completed == 0)
        precondition(unchangedStatistics.snoozes == 0 && unchangedStatistics.fallback == 0)
        precondition(unchangedStatistics.totalCompletionSeconds == 0)
        let restartedAgain = CoreDataStore(storeURL: storeURL)
        let active = await restartedAgain.fetchActiveWakeSession()
        precondition(active?.id == session.id)
        var closedSession = session
        closedSession.completedAt = .now
        closedSession.result = "COMPLETED"
        await restartedAgain.saveWakeSession(closedSession)

        let manager = await MainActor.run { WakeSessionManager(database: restartedAgain) }
        let alarm = Alarm(
            id: UUID(),
            version: 1,
            hour: 7,
            minute: 30,
            enabled: true,
            weekdays: Set(1...7),
            timezoneMode: .deviceLocal,
            fixedTimezone: nil,
            missionType: .math,
            difficulty: .medium
        )
        await restartedAgain.saveAlarm(alarm)
        async let firstTrigger = manager.trigger(alarm: alarm, scheduledAt: .now)
        async let secondTrigger = manager.trigger(alarm: alarm, scheduledAt: .now)
        let triggerResults = await [firstTrigger, secondTrigger]
        precondition(triggerResults.filter { $0 }.count == 1)
        await manager.startMission()
        await manager.startMission()

        let restoredManager = await MainActor.run { WakeSessionManager(database: restartedAgain) }
        let restoredFlow = await MainActor.run {
            WakeFlowController(sessionManager: restoredManager, database: restartedAgain)
        }
        await restoredFlow.restore()
        let restoredState = await MainActor.run { restoredFlow.state }
        precondition(restoredState == .mission)

        async let firstCompletion = manager.complete()
        async let secondCompletion = manager.complete()
        let completions = await [firstCompletion, secondCompletion].compactMap { $0 }
        precondition(completions.count == 1)
        let completedSession = completions[0]

        let statistics = await MainActor.run { StatisticsStore(database: restartedAgain) }
        async let firstPlanned: Void = statistics.recordPlanned()
        async let secondPlanned: Void = statistics.recordPlanned()
        _ = await (firstPlanned, secondPlanned)
        await statistics.record(completedSession)

        let finalStore = CoreDataStore(storeURL: storeURL)
        let finalStatistics = await finalStore.fetchStatistics()
        precondition(finalStatistics?.planned == 2)
        precondition(finalStatistics?.completed == 1)
        let finalActive = await finalStore.fetchActiveWakeSession()
        precondition(finalActive == nil)

        let e2eAlarm = Alarm(
            id: UUID(),
            version: 1,
            hour: 6,
            minute: 30,
            enabled: true,
            weekdays: Set(1...7),
            timezoneMode: .deviceLocal,
            fixedTimezone: nil,
            missionType: .math,
            difficulty: .easy
        )
        await finalStore.saveAlarm(e2eAlarm)
        let e2eController = await MainActor.run {
            WakeFlowController(
                sessionManager: WakeSessionManager(database: finalStore),
                database: finalStore
            )
        }
        await e2eController.start(alarm: e2eAlarm, scheduledAt: .now)
        await e2eController.beginMission()
        let e2eState = await MainActor.run { e2eController.state }
        precondition(e2eState == .mission)

        let e2eRestoredController = await MainActor.run {
            WakeFlowController(
                sessionManager: WakeSessionManager(database: finalStore),
                database: finalStore
            )
        }
        await e2eRestoredController.restore()
        let e2eRestoredState = await MainActor.run { e2eRestoredController.state }
        precondition(e2eRestoredState == .mission)
        await e2eRestoredController.completeMission()
        let e2eCompletedState = await MainActor.run { e2eRestoredController.state }
        precondition(e2eCompletedState == .completed)

        let e2eStats = await finalStore.fetchStatistics()
        precondition(e2eStats?.planned == 3)
        precondition(e2eStats?.completed == 2)

        let emergencyAlarm = Alarm(
            id: UUID(),
            version: 1,
            hour: 8,
            minute: 0,
            enabled: true,
            weekdays: Set(1...7),
            timezoneMode: .deviceLocal,
            fixedTimezone: nil,
            missionType: .math,
            difficulty: .easy
        )
        await finalStore.saveAlarm(emergencyAlarm)
        let emergencyManager = await MainActor.run { WakeSessionManager(database: finalStore) }
        let emergencyTriggered = await emergencyManager.trigger(alarm: emergencyAlarm, scheduledAt: .now)
        precondition(emergencyTriggered)
        guard let emergencySession = await emergencyManager.emergencyStop() else {
            fatalError("Expected emergency stop session")
        }
        precondition(emergencySession.result == "EMERGENCY_STOP")
        let emergencyRestart = CoreDataStore(storeURL: storeURL)
        let emergencyActive = await emergencyRestart.fetchActiveWakeSession()
        precondition(emergencyActive == nil)

        try await runAlarmRecoveryChecks(root: root)
        print("AWERO persistence smoke: PASS")
        try? FileManager.default.removeItem(at: root)
    }

    @MainActor
    private static func runAlarmRecoveryChecks(root: URL) async throws {
        let url = root.appendingPathComponent("alarm-recovery.sqlite")
        let database = CoreDataStore(storeURL: url)
        let enabled = Alarm(version: 3, hour: 9, minute: 15, weekdays: [1, 3],
                            timezoneMode: .fixed, fixedTimezone: "Europe/Moscow")
        var disabled = Alarm(hour: 10, minute: 0, enabled: false, weekdays: [2, 4])
        let enabledSaved = await database.saveAlarm(enabled)
        let disabledSaved = await database.saveAlarm(disabled)
        precondition(enabledSaved && disabledSaved)

        let center = SmokeNotificationCenter()
        let scheduler = AlarmScheduler(center: center)
        var stale = enabled
        stale.version = 2
        try await scheduler.schedule(stale)
        disabled.enabled = true
        try await scheduler.schedule(disabled)
        disabled.enabled = false

        let restarted = CoreDataStore(storeURL: url)
        let store = AlarmStore(database: restarted)
        await store.load()
        let recovery = AlarmRecovery(scheduler: scheduler, store: store)
        await recovery.reconcile()
        let requests = await center.pendingNotificationRequests()
        precondition(requests.count == 2)
        precondition(Set(requests.map(\.identifier)) == Set(enabled.weekdays.map {
            "awero:alarm:\(enabled.id.uuidString):v3:w\($0)"
        }))
        for request in requests {
            let trigger = request.trigger as! UNCalendarNotificationTrigger
            precondition(trigger.repeats)
            precondition(trigger.dateComponents.hour == 9 && trigger.dateComponents.minute == 15)
            precondition(trigger.dateComponents.timeZone?.identifier == "Europe/Moscow")
        }
        let additions = center.additions
        await recovery.reconcile()
        precondition(center.additions == additions)

        let readOnly = AlarmStore(database: CoreDataStore(storeURL: url, readOnly: true))
        await readOnly.load()
        let coordinator = AlarmCoordinator(store: readOnly, scheduler: scheduler)
        do {
            try await coordinator.update(enabled)
            preconditionFailure("Expected read-only alarm update to fail")
        } catch AlarmCoordinatorError.persistenceFailed {
            // The database rejects the write before scheduling can change.
        }
        await coordinator.delete(enabled)
        do {
            try await coordinator.create(Alarm(hour: 11, minute: 0))
            preconditionFailure("Expected read-only alarm creation to fail")
        } catch AlarmCoordinatorError.persistenceFailed {
            // The database rejects the write before scheduling can change.
        }
        let unchanged = await center.pendingNotificationRequests()
        precondition(Set(unchanged.map(\.identifier)) == Set(requests.map(\.identifier)))
        precondition(center.additions == additions)
        precondition(readOnly.alarms.count == 2 && readOnly.alarms.contains { $0.id == enabled.id })

        center.removePendingNotificationRequests(withIdentifiers: requests.map(\.identifier))
        center.failNextAdd = true
        await recovery.reconcile()
        await recovery.reconcile()
        let repaired = await scheduler.isScheduled(enabled)
        precondition(repaired)
        let repairedRequests = await center.pendingNotificationRequests()
        precondition(repairedRequests.count == 2)
        let defaults = UserDefaults.standard
        let legacyKey = "awero.statistics.v1"
        let migratedKey = "awero.coredata.statistics.migrated.v1"
        let previousLegacy = defaults.object(forKey: legacyKey)
        let previousMarker = defaults.object(forKey: migratedKey)
        defer {
            if let previousLegacy { defaults.set(previousLegacy, forKey: legacyKey) }
            else { defaults.removeObject(forKey: legacyKey) }
            if let previousMarker { defaults.set(previousMarker, forKey: migratedKey) }
            else { defaults.removeObject(forKey: migratedKey) }
        }
        var legacyStatistics = WakeStatistics()
        legacyStatistics.planned = 4
        legacyStatistics.completed = 2
        defaults.set(try JSONEncoder().encode(legacyStatistics), forKey: legacyKey)
        defaults.set(false, forKey: migratedKey)
        let failedMigration = StatisticsStore(database: CoreDataStore(storeURL: url, readOnly: true))
        await failedMigration.load()
        precondition(!defaults.bool(forKey: migratedKey))
        precondition(failedMigration.statistics.planned == 0)
        let missingStatistics = await restarted.fetchStatistics()
        precondition(missingStatistics == nil)
        let retriedMigration = StatisticsStore(database: restarted)
        await retriedMigration.load()
        precondition(defaults.bool(forKey: migratedKey))
        let migratedStatistics = await restarted.fetchStatistics()
        precondition(migratedStatistics?.planned == 4 && migratedStatistics?.completed == 2)
        print("AWERO alarm recovery and failed-write scheduling: PASS")
        print("AWERO statistics migration failure and retry: PASS")
    }

    private static func runCrashPhase(_ phase: String) async throws {
        guard let path = ProcessInfo.processInfo.environment["AWERO_CRASH_PATH"] else {
            fatalError("AWERO_CRASH_PATH is required")
        }
        let storeURL = URL(fileURLWithPath: path)

        if phase == "write" {
            let store = CoreDataStore(storeURL: storeURL)
            let alarm = Alarm(
                id: UUID(),
                version: 1,
                hour: 7,
                minute: 0,
                enabled: true,
                weekdays: Set(1...7),
                timezoneMode: .deviceLocal,
                fixedTimezone: nil,
                missionType: .math,
                difficulty: .medium
            )
            let alarmSaved = await store.saveAlarm(alarm)
            precondition(alarmSaved)
            let session = WakeSession(
                id: UUID(),
                alarmId: alarm.id,
                alarmVersion: alarm.version,
                scheduledAt: .now,
                triggeredAt: .now,
                missionStartedAt: .now,
                completedAt: nil,
                result: nil,
                missionType: .math,
                completionTimeSeconds: nil,
                snoozeCount: 1,
                fallbackUsed: true,
                emergencyStop: false
            )
            let sessionSaved = await store.saveWakeSession(session)
            precondition(sessionSaved)
            let operation = SyncOperation(
                id: UUID(uuidString: "00000000-0000-0000-0000-000000000001")!,
                operationType: "UPDATE_ALARM",
                entityType: "ALARM",
                entityId: alarm.id.uuidString,
                clientVersion: 1,
                payload: ["hour": "7"]
            )
            let operationSaved = await store.saveSyncOperation(operation)
            let duplicateSaved = await store.saveSyncOperation(operation)
            let retrySaved = await store.retrySyncOperation(
                operation.id, nextAttemptAt: Date(timeIntervalSinceNow: 3600)
            )
            precondition(operationSaved && duplicateSaved && retrySaved)
            try Data("READY".utf8).write(to: storeURL.appendingPathExtension("ready"), options: .atomic)
            print("AWERO crash phase write: READY")
            while true {
                try await Task.sleep(nanoseconds: 60_000_000_000)
            }
        }

        if phase == "read" {
            let store = CoreDataStore(storeURL: storeURL)
            let alarms = await store.fetchAlarms()
            precondition(alarms.count == 1)
            precondition(alarms[0].hour == 7)
            let session = await store.fetchActiveWakeSession()
            precondition(session != nil)
            precondition(session?.missionStartedAt != nil)
            precondition(session?.snoozeCount == 1)
            precondition(session?.fallbackUsed == true)
            let blockedRetry = await store.fetchDueSyncOperations()
            precondition(blockedRetry.isEmpty)
            let operationId = UUID(uuidString: "00000000-0000-0000-0000-000000000001")!
            let retrySaved = await store.retrySyncOperation(operationId, nextAttemptAt: .distantPast)
            precondition(retrySaved)
            let recoveredQueue = await store.fetchDueSyncOperations()
            precondition(recoveredQueue.count == 1 && recoveredQueue[0].id == operationId)
            let duplicateSaved = await store.saveSyncOperation(recoveredQueue[0])
            precondition(duplicateSaved)
            let stillUnique = await store.fetchDueSyncOperations()
            precondition(stillUnique.count == 1)
            let controller = await MainActor.run {
                WakeFlowController(sessionManager: WakeSessionManager(database: store), database: store)
            }
            await controller.restore()
            let restoredState = await MainActor.run { controller.state }
            precondition(restoredState == .mission)
            print("AWERO crash phase read: PASS")
            return
        }

        fatalError("Unknown AWERO_CRASH_PHASE")
    }

    private static func load(_ container: NSPersistentContainer) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            container.loadPersistentStores { _, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume()
                }
            }
        }
    }
}

private final class SmokeNotificationCenter: AlarmNotificationCenter {
    private var requests: [String: UNNotificationRequest] = [:]
    private(set) var additions = 0
    var failNextAdd = false

    func requestAuthorization(options: UNAuthorizationOptions) async throws -> Bool { true }

    func add(_ request: UNNotificationRequest) async throws {
        if failNextAdd {
            failNextAdd = false
            throw NSError(domain: "AWERO.SmokeScheduling", code: 1)
        }
        requests[request.identifier] = request
        additions += 1
    }

    func pendingNotificationRequests() async -> [UNNotificationRequest] {
        Array(requests.values)
    }

    func removePendingNotificationRequests(withIdentifiers identifiers: [String]) {
        for id in identifiers { requests.removeValue(forKey: id) }
    }
}
