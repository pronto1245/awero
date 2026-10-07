import CoreData
import Foundation
import os

enum PersistenceError: Error {
    case saveFailed(underlying: Error)
}

final class CoreDataStore: @unchecked Sendable {
    static let shared = CoreDataStore()

    private let container: NSPersistentContainer
    private let logger = Logger(subsystem: "app.awero", category: "persistence")

    init(storeURL: URL? = nil) {
        let model = Self.makeModel()
        container = NSPersistentContainer(name: "AWERO", managedObjectModel: model)

        let storeURL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("AWERO.sqlite")
        do {
            try FileManager.default.createDirectory(
                at: storeURL.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
        } catch {
            fatalError("AWERO Core Data directory failed: \(error.localizedDescription)")
        }

        let description = NSPersistentStoreDescription(url: resolvedStoreURL)
        description.shouldMigrateStoreAutomatically = true
        description.shouldInferMappingModelAutomatically = true
        container.persistentStoreDescriptions = [description]

        var loadError: Error?
        let semaphore = DispatchSemaphore(value: 0)
        container.loadPersistentStores { _, error in
            loadError = error
            semaphore.signal()
        }
        semaphore.wait()
        if let loadError {
            fatalError("AWERO Core Data store failed: \(loadError.localizedDescription)")
        }
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        container.viewContext.undoManager = nil
    }

    func saveAlarm(_ alarm: Alarm) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.predicate = NSPredicate(format: "id == %@", alarm.id.uuidString)
            let object = (try? context.fetch(request).first) ?? NSManagedObject(
                entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["AlarmRecord"]!,
                insertInto: context
            )
            object.setValue(alarm.id.uuidString, forKey: "id")
            object.setValue(alarm.version, forKey: "version")
            object.setValue(alarm.hour, forKey: "hour")
            object.setValue(alarm.minute, forKey: "minute")
            object.setValue(alarm.enabled, forKey: "enabled")
            object.setValue(alarm.weekdays.sorted().map(String.init).joined(separator: ","), forKey: "weekdays")
            object.setValue(alarm.timezoneMode.rawValue, forKey: "timezoneMode")
            object.setValue(alarm.fixedTimezone, forKey: "fixedTimezone")
            object.setValue(alarm.missionType.rawValue, forKey: "missionType")
            object.setValue(alarm.difficulty.rawValue, forKey: "difficulty")
            object.setValue(alarm.maxSnoozes, forKey: "maxSnoozes")
            object.setValue(alarm.snoozeMinutes, forKey: "snoozeMinutes")
            object.setValue(alarm.qrExpectedCode, forKey: "qrExpectedCode")
        }
    }

    func fetchAlarm(id: UUID) async -> Alarm? {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.predicate = NSPredicate(format: "id == %@", id.uuidString)
            return try? context.fetch(request).first.flatMap(Self.alarm(from:))
        }
    }

    func fetchAlarms() async -> [Alarm] {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.sortDescriptors = [
                NSSortDescriptor(key: "hour", ascending: true),
                NSSortDescriptor(key: "minute", ascending: true)
            ]
            return (try? context.fetch(request).compactMap(Self.alarm(from:))) ?? []
        }
    }

    func saveSyncOperation(_ operation: SyncOperation) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            request.predicate = NSPredicate(format: "id == %@", operation.id.uuidString)
            guard (try? context.fetch(request).first) == nil else { return }

            let object = NSManagedObject(entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["SyncOperationRecord"]!, insertInto: context)
            object.setValue(operation.id.uuidString, forKey: "id")
            object.setValue(operation.operationType, forKey: "operationType")
            object.setValue(operation.entityType, forKey: "entityType")
            object.setValue(operation.entityId, forKey: "entityId")
            object.setValue(operation.clientVersion, forKey: "clientVersion")
            object.setValue((try? JSONEncoder().encode(operation.payload)).flatMap { String(data: $0, encoding: .utf8) } ?? "{}", forKey: "payload")
            object.setValue(operation.occurredAt, forKey: "occurredAt")
            object.setValue(0, forKey: "attempts")
            object.setValue(Date.distantPast, forKey: "nextAttemptAt")
        }
    }

    func fetchDueSyncOperations() async -> [SyncOperation] {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            request.predicate = NSPredicate(format: "nextAttemptAt <= %@", Date.now as NSDate)
            request.sortDescriptors = [NSSortDescriptor(key: "occurredAt", ascending: true)]
            request.fetchLimit = 100
            return (try? context.fetch(request).compactMap(Self.syncOperation(from:))) ?? []
        }
    }

    func deleteSyncOperation(_ id: UUID) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            request.predicate = NSPredicate(format: "id == %@", id.uuidString)
            if let object = try? context.fetch(request).first { context.delete(object) }
        }
    }

    func retrySyncOperation(_ id: UUID, nextAttemptAt: Date) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            request.predicate = NSPredicate(format: "id == %@", id.uuidString)
            if let object = try? context.fetch(request).first {
                let attempts = object.value(forKey: "attempts") as? Int ?? 0
                object.setValue(attempts + 1, forKey: "attempts")
                object.setValue(nextAttemptAt, forKey: "nextAttemptAt")
            }
        }
    }

    func saveAnalyticsEvent(_ event: AnalyticsEvent) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AnalyticsEventRecord")
            request.predicate = NSPredicate(format: "id == %@", event.id.uuidString)
            guard (try? context.fetch(request).first) == nil else { return }

            let object = NSManagedObject(entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["AnalyticsEventRecord"]!, insertInto: context)
            object.setValue(event.id.uuidString, forKey: "id")
            object.setValue(event.name, forKey: "name")
            object.setValue(event.version, forKey: "version")
            object.setValue((try? JSONEncoder().encode(event.payload)).flatMap { String(data: $0, encoding: .utf8) } ?? "{}", forKey: "payload")
            object.setValue(event.occurredAt, forKey: "occurredAt")
        }
    }

    func fetchPendingAnalyticsEvents() async -> [AnalyticsEvent] {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AnalyticsEventRecord")
            request.sortDescriptors = [NSSortDescriptor(key: "occurredAt", ascending: true)]
            request.fetchLimit = 100
            return (try? context.fetch(request).compactMap(Self.analyticsEvent(from:))) ?? []
        }
    }

    func deleteAnalyticsEvent(_ id: UUID) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AnalyticsEventRecord")
            request.predicate = NSPredicate(format: "id == %@", id.uuidString)
            if let object = try? context.fetch(request).first { context.delete(object) }
        }
    }

    func saveStatistics(_ statistics: WakeStatistics) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "StatisticsRecord")
            request.predicate = NSPredicate(format: "id == %@", "singleton")
            let object = (try? context.fetch(request).first) ?? NSManagedObject(
                entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["StatisticsRecord"]!,
                insertInto: context
            )
            object.setValue("singleton", forKey: "id")
            object.setValue(statistics.planned, forKey: "planned")
            object.setValue(statistics.completed, forKey: "completed")
            object.setValue(statistics.snoozes, forKey: "snoozes")
            object.setValue(statistics.fallback, forKey: "fallback")
            object.setValue(statistics.emergencyStops, forKey: "emergencyStops")
            object.setValue(statistics.totalCompletionSeconds, forKey: "totalCompletionSeconds")
        }
    }

    func fetchStatistics() async -> WakeStatistics? {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "StatisticsRecord")
            request.predicate = NSPredicate(format: "id == %@", "singleton")
            guard let object = try? context.fetch(request).first else { return nil }
            return WakeStatistics(
                planned: object.value(forKey: "planned") as? Int ?? 0,
                completed: object.value(forKey: "completed") as? Int ?? 0,
                snoozes: object.value(forKey: "snoozes") as? Int ?? 0,
                fallback: object.value(forKey: "fallback") as? Int ?? 0,
                emergencyStops: object.value(forKey: "emergencyStops") as? Int ?? 0,
                totalCompletionSeconds: object.value(forKey: "totalCompletionSeconds") as? Int ?? 0
            )
        }
    }

    func saveWakeSession(_ session: WakeSession) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "WakeSessionRecord")
            request.predicate = NSPredicate(format: "id == %@", session.id.uuidString)
            let object = (try? context.fetch(request).first) ?? NSManagedObject(
                entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["WakeSessionRecord"]!,
                insertInto: context
            )
            object.setValue(session.id.uuidString, forKey: "id")
            object.setValue(session.alarmId.uuidString, forKey: "alarmId")
            object.setValue(session.alarmVersion, forKey: "alarmVersion")
            object.setValue(session.scheduledAt, forKey: "scheduledAt")
            object.setValue(session.triggeredAt, forKey: "triggeredAt")
            object.setValue(session.missionStartedAt, forKey: "missionStartedAt")
            object.setValue(session.completedAt, forKey: "completedAt")
            object.setValue(session.result, forKey: "result")
            object.setValue(session.missionType.rawValue, forKey: "missionType")
            object.setValue(session.completionTimeSeconds, forKey: "completionTimeSeconds")
            object.setValue(session.snoozeCount, forKey: "snoozeCount")
            object.setValue(session.fallbackUsed, forKey: "fallbackUsed")
            object.setValue(session.emergencyStop, forKey: "emergencyStop")
        }
    }

    func fetchActiveWakeSession() async -> WakeSession? {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "WakeSessionRecord")
            request.predicate = NSPredicate(format: "completedAt == nil")
            request.sortDescriptors = [NSSortDescriptor(key: "scheduledAt", ascending: false)]
            return try? context.fetch(request).first.flatMap(Self.wakeSession(from:))
        }
    }

    func deleteAlarm(_ alarm: Alarm) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.predicate = NSPredicate(format: "id == %@", alarm.id.uuidString)
            if let object = try? context.fetch(request).first { context.delete(object) }
        }
    }

    private func performBackground<T: Sendable>(
        _ work: @escaping @Sendable (NSManagedObjectContext) -> T
    ) async -> T {
        await container.performBackgroundTask { context in
            let result = work(context)
            if context.hasChanges {
                do {
                    try context.save()
                } catch {
                    logger.error("Core Data save failed: \(error.localizedDescription, privacy: .public)")
                    NotificationCenter.default.post(
                        name: .aweroPersistenceSaveFailed,
                        object: nil,
                        userInfo: ["error": error]
                    )
                }
            }
            return result
        }
    }

    private static func syncOperation(from object: NSManagedObject) -> SyncOperation? {
        guard let id = UUID(uuidString: object.value(forKey: "id") as? String ?? "") else { return nil }
        let payloadData = Data((object.value(forKey: "payload") as? String ?? "{}").utf8)
        let payload = (try? JSONDecoder().decode([String: String].self, from: payloadData)) ?? [:]
        return SyncOperation(id: id, operationType: object.value(forKey: "operationType") as? String ?? "", entityType: object.value(forKey: "entityType") as? String ?? "", entityId: object.value(forKey: "entityId") as? String ?? "", clientVersion: object.value(forKey: "clientVersion") as? Int, payload: payload, occurredAt: object.value(forKey: "occurredAt") as? Date ?? .now)
    }

    private static func analyticsEvent(from object: NSManagedObject) -> AnalyticsEvent? {
        guard let id = UUID(uuidString: object.value(forKey: "id") as? String ?? "") else { return nil }
        let payloadData = Data((object.value(forKey: "payload") as? String ?? "{}").utf8)
        let payload = (try? JSONDecoder().decode([String: String].self, from: payloadData)) ?? [:]
        return AnalyticsEvent(id: id, name: object.value(forKey: "name") as? String ?? "", version: object.value(forKey: "version") as? Int ?? 1, payload: payload, occurredAt: object.value(forKey: "occurredAt") as? Date ?? .now)
    }

    private static func wakeSession(from object: NSManagedObject) -> WakeSession? {
        guard
            let idString = object.value(forKey: "id") as? String,
            let id = UUID(uuidString: idString),
            let alarmIdString = object.value(forKey: "alarmId") as? String,
            let alarmId = UUID(uuidString: alarmIdString),
            let missionRaw = object.value(forKey: "missionType") as? String,
            let mission = MissionType(rawValue: missionRaw)
        else { return nil }

        return WakeSession(
            id: id, alarmId: alarmId, alarmVersion: object.value(forKey: "alarmVersion") as? Int ?? 1,
            scheduledAt: object.value(forKey: "scheduledAt") as? Date ?? .now,
            triggeredAt: object.value(forKey: "triggeredAt") as? Date,
            missionStartedAt: object.value(forKey: "missionStartedAt") as? Date,
            completedAt: object.value(forKey: "completedAt") as? Date,
            result: object.value(forKey: "result") as? String, missionType: mission,
            completionTimeSeconds: object.value(forKey: "completionTimeSeconds") as? Int,
            snoozeCount: object.value(forKey: "snoozeCount") as? Int ?? 0,
            fallbackUsed: object.value(forKey: "fallbackUsed") as? Bool ?? false,
            emergencyStop: object.value(forKey: "emergencyStop") as? Bool ?? false
        )
    }

    private static func alarm(from object: NSManagedObject) -> Alarm? {
        guard
            let idString = object.value(forKey: "id") as? String,
            let id = UUID(uuidString: idString),
            let timezoneRaw = object.value(forKey: "timezoneMode") as? String,
            let timezone = AlarmTimezoneMode(rawValue: timezoneRaw),
            let missionRaw = object.value(forKey: "missionType") as? String,
            let mission = MissionType(rawValue: missionRaw),
            let difficultyRaw = object.value(forKey: "difficulty") as? String,
            let difficulty = Difficulty(rawValue: difficultyRaw)
        else { return nil }

        let days = Set((object.value(forKey: "weekdays") as? String ?? "")
            .split(separator: ",")
            .compactMap { Int($0) })

        return Alarm(
            id: id,
            version: object.value(forKey: "version") as? Int ?? 1,
            hour: object.value(forKey: "hour") as? Int ?? 7,
            minute: object.value(forKey: "minute") as? Int ?? 30,
            enabled: object.value(forKey: "enabled") as? Bool ?? true,
            weekdays: days,
            timezoneMode: timezone,
            fixedTimezone: object.value(forKey: "fixedTimezone") as? String,
            missionType: mission,
            difficulty: difficulty,
            maxSnoozes: object.value(forKey: "maxSnoozes") as? Int ?? 3,
            snoozeMinutes: object.value(forKey: "snoozeMinutes") as? Int ?? 10,
            qrExpectedCode: object.value(forKey: "qrExpectedCode") as? String
        )
    }

    static func makeModelForTesting() -> NSManagedObjectModel {\n        makeModel()\n    }\n\n    private static func makeModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()
        model.entities = [
            entity(name: "AlarmRecord", attributes: [
                ("id", .stringAttributeType, false),
                ("version", .integer64AttributeType, false),
                ("hour", .integer64AttributeType, false),
                ("minute", .integer64AttributeType, false),
                ("enabled", .booleanAttributeType, false),
                ("weekdays", .stringAttributeType, false),
                ("timezoneMode", .stringAttributeType, false),
                ("fixedTimezone", .stringAttributeType, true),
                ("missionType", .stringAttributeType, false),
                ("difficulty", .stringAttributeType, false),
                ("maxSnoozes", .integer64AttributeType, false),
                ("snoozeMinutes", .integer64AttributeType, false),
                ("qrExpectedCode", .stringAttributeType, true)
            ]),
            entity(name: "SyncOperationRecord", attributes: [
                ("id", .stringAttributeType, false), ("operationType", .stringAttributeType, false), ("entityType", .stringAttributeType, false), ("entityId", .stringAttributeType, false), ("clientVersion", .integer64AttributeType, true), ("payload", .stringAttributeType, false), ("occurredAt", .dateAttributeType, false), ("attempts", .integer64AttributeType, false), ("nextAttemptAt", .dateAttributeType, false)
            ]),
            entity(name: "AnalyticsEventRecord", attributes: [
                ("id", .stringAttributeType, false), ("name", .stringAttributeType, false), ("version", .integer64AttributeType, false), ("payload", .stringAttributeType, false), ("occurredAt", .dateAttributeType, false)
            ]),
            entity(name: "StatisticsRecord", attributes: [
                ("id", .stringAttributeType, false),
                ("planned", .integer64AttributeType, false),
                ("completed", .integer64AttributeType, false),
                ("snoozes", .integer64AttributeType, false),
                ("fallback", .integer64AttributeType, false),
                ("emergencyStops", .integer64AttributeType, false),
                ("totalCompletionSeconds", .integer64AttributeType, false)
            ]),
            entity(name: "WakeSessionRecord", attributes: [
                ("id", .stringAttributeType, false),
                ("alarmId", .stringAttributeType, false),
                ("alarmVersion", .integer64AttributeType, false),
                ("scheduledAt", .dateAttributeType, false),
                ("triggeredAt", .dateAttributeType, true),
                ("missionStartedAt", .dateAttributeType, true),
                ("completedAt", .dateAttributeType, true),
                ("result", .stringAttributeType, true),
                ("missionType", .stringAttributeType, false),
                ("completionTimeSeconds", .integer64AttributeType, true),
                ("snoozeCount", .integer64AttributeType, false),
                ("fallbackUsed", .booleanAttributeType, false),
                ("emergencyStop", .booleanAttributeType, false)
            ])
        ]
        return model
    }

    private static func entity(
        name: String,
        attributes: [(String, NSAttributeType, Bool)]
    ) -> NSEntityDescription {
        let entity = NSEntityDescription()
        entity.name = name
        entity.managedObjectClassName = "NSManagedObject"
        entity.properties = attributes.map { name, type, optional in
            let attribute = NSAttributeDescription()
            attribute.name = name
            attribute.attributeType = type
            attribute.isOptional = optional
            return attribute
        }
        return entity
    }
}

extension Notification.Name {
    static let aweroPersistenceSaveFailed = Notification.Name("awero.persistence.save.failed")
}
