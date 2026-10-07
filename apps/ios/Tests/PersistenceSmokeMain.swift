import Foundation
import CoreData

@main
struct PersistenceSmokeMain {
    static func main() async throws {
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

        let session = WakeSession(
            id: UUID(),
            alarmId: alarmId,
            alarmVersion: 1,
            scheduledAt: .now,
            triggeredAt: .now,
            missionStartedAt: .now,
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
        let firstTrigger = await manager.trigger(alarm: alarm, scheduledAt: .now)
        let secondTrigger = await manager.trigger(alarm: alarm, scheduledAt: .now)
        precondition(firstTrigger)
        precondition(!secondTrigger)
        await manager.startMission()
        await manager.startMission()
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
        precondition(await finalStore.fetchActiveWakeSession() == nil)

        print("AWERO persistence smoke: PASS")
        try? FileManager.default.removeItem(at: root)
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
