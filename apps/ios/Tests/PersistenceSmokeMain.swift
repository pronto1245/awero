import Foundation
import CoreData

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
        await restarted.saveWakeSession(session)
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
        let firstTrigger = await manager.trigger(alarm: alarm, scheduledAt: .now)
        let secondTrigger = await manager.trigger(alarm: alarm, scheduledAt: .now)
        precondition(firstTrigger)
        precondition(!secondTrigger)
        await manager.startMission()
        await manager.startMission()

        let restoredManager = await MainActor.run { WakeSessionManager(database: restartedAgain) }
        let restoredFlow = await MainActor.run {
            WakeFlowController(sessionManager: restoredManager, database: restartedAgain)
        }
        await restoredFlow.restore()
        let restoredState = await MainActor.run { restoredFlow.state }
        precondition(restoredState == .mission)

        guard let completedSession = await manager.complete() else {
            fatalError("Expected wake session completion")
        }
        let duplicateCompletion = await manager.complete()
        precondition(duplicateCompletion == nil)

        let statistics = await MainActor.run { StatisticsStore(database: restartedAgain) }
        await statistics.recordPlanned()
        await statistics.record(completedSession)

        let finalStore = CoreDataStore(storeURL: storeURL)
        let finalStatistics = await finalStore.fetchStatistics()
        precondition(finalStatistics?.planned == 1)
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
        precondition(e2eStats?.planned == 2)
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

        print("AWERO persistence smoke: PASS")
        try? FileManager.default.removeItem(at: root)
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
            await store.saveAlarm(alarm)
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
            await store.saveWakeSession(session)
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
