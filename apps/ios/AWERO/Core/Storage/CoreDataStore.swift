import CoreData
import Foundation
import os

enum PersistenceError: Error {
    case saveFailed(underlying: Error)
    case readFailed(underlying: Error)
    case storeUnavailable
}

final class CoreDataStore: @unchecked Sendable {
    static let shared = CoreDataStore()

    private let container: NSPersistentContainer
    private let persistentStoreReady: Task<Void, Error>
    private let logger = Logger(subsystem: "app.awero", category: "persistence")

    init(storeURL: URL? = nil, readOnly: Bool = false) {
        let model = Self.makeModel()
        container = NSPersistentContainer(name: "AWERO", managedObjectModel: model)

        let resolvedStoreURL = storeURL ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("AWERO.sqlite")
        do {
            try FileManager.default.createDirectory(
                at: resolvedStoreURL.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
        } catch {
            fatalError("AWERO Core Data directory failed: \(error.localizedDescription)")
        }

        let description = NSPersistentStoreDescription(url: resolvedStoreURL)
        description.shouldMigrateStoreAutomatically = true
        description.shouldInferMappingModelAutomatically = true
        if readOnly {
            description.setOption(["query_only": "1"] as NSDictionary, forKey: NSSQLitePragmasOption)
        }
        container.persistentStoreDescriptions = [description]

        let persistentContainer = container
        persistentStoreReady = Task {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                persistentContainer.loadPersistentStores { _, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else {
                        continuation.resume()
                    }
                }
            }
        }
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        container.viewContext.undoManager = nil
    }

    func saveAlarm(_ alarm: Alarm) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.predicate = NSPredicate(format: "id == %@", alarm.id.uuidString)
            let object: NSManagedObject
            if let existing = try context.fetch(request).first {
                object = existing
            } else {
                object = NSManagedObject(
                    entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["AlarmRecord"]!,
                    insertInto: context
                )
            }
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
            object.setValue(alarm.label, forKey: "label")
            object.setValue(alarm.snoozeEnabled, forKey: "snoozeEnabled")
            object.setValue(alarm.qrExpectedCode, forKey: "qrExpectedCode")
        }
    }

    func fetchAlarm(id: UUID) async -> Result<Alarm?, PersistenceError> {
        await performRead { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.predicate = NSPredicate(format: "id == %@", id.uuidString)
            guard let object = try context.fetch(request).first else { return nil }
            guard let alarm = Self.alarm(from: object) else { throw Self.invalidRecord() }
            return alarm
        }
    }

    func fetchAlarms() async -> Result<[Alarm], PersistenceError> {
        await performRead { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.sortDescriptors = [
                NSSortDescriptor(key: "hour", ascending: true),
                NSSortDescriptor(key: "minute", ascending: true)
            ]
            return try Self.requireAllMapped(context.fetch(request), transform: Self.alarm(from:))
        }
    }

    func saveSyncOperation(_ operation: SyncOperation) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            request.predicate = NSPredicate(format: "id == %@", operation.id.uuidString)
            guard try context.fetch(request).first == nil else { return }

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

    func fetchDueSyncOperations() async -> Result<[SyncOperation], PersistenceError> {
        await performRead { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            request.predicate = NSPredicate(format: "nextAttemptAt <= %@", Date.now as NSDate)
            request.sortDescriptors = [NSSortDescriptor(key: "occurredAt", ascending: true)]
            request.fetchLimit = 100
            return try Self.requireAllMapped(context.fetch(request), transform: Self.syncOperation(from:))
        }
    }

    func deleteSyncOperation(_ id: UUID) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            request.predicate = NSPredicate(format: "id == %@", id.uuidString)
            if let object = try context.fetch(request).first { context.delete(object) }
        }
    }

    func retrySyncOperation(_ id: UUID, nextAttemptAt: Date? = nil) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            request.predicate = NSPredicate(format: "id == %@", id.uuidString)
            if let object = try context.fetch(request).first {
                let attempts = object.value(forKey: "attempts") as? Int ?? 0
                let delay = Double(1 << min(max(attempts, 0), 6))
                object.setValue(attempts + 1, forKey: "attempts")
                object.setValue(nextAttemptAt ?? Date.now.addingTimeInterval(delay), forKey: "nextAttemptAt")
            }
        }
    }

    func recordSyncConflict(_ operation: SyncOperation, conflict: SyncConflict) async -> Bool {
        await performWrite { context in
            let conflictRequest = NSFetchRequest<NSManagedObject>(entityName: "SyncConflictRecord")
            conflictRequest.predicate = NSPredicate(format: "operationId == %@", operation.id.uuidString)
            let object = try context.fetch(conflictRequest).first ?? NSManagedObject(
                entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["SyncConflictRecord"]!,
                insertInto: context
            )
            object.setValue(operation.id.uuidString, forKey: "operationId")
            object.setValue(operation.operationType, forKey: "operationType")
            object.setValue(operation.entityType, forKey: "entityType")
            object.setValue(operation.entityId, forKey: "entityId")
            object.setValue(operation.clientVersion, forKey: "clientVersion")
            object.setValue((try? JSONEncoder().encode(operation.payload)).flatMap { String(data: $0, encoding: .utf8) } ?? "{}", forKey: "localPayload")
            object.setValue(operation.occurredAt, forKey: "occurredAt")
            object.setValue(conflict.code, forKey: "code")
            object.setValue(conflict.serverVersion, forKey: "serverVersion")
            object.setValue(conflict.serverEntity.flatMap { try? JSONEncoder().encode($0) }.flatMap { String(data: $0, encoding: .utf8) }, forKey: "serverEntityJson")
            object.setValue(Date.now, forKey: "detectedAt")

            let operationRequest = NSFetchRequest<NSManagedObject>(entityName: "SyncOperationRecord")
            operationRequest.predicate = NSPredicate(format: "id == %@", operation.id.uuidString)
            if let queued = try context.fetch(operationRequest).first { context.delete(queued) }
        }
    }

    func fetchSyncConflicts() async -> Result<[SyncConflictRecord], PersistenceError> {
        await performRead { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncConflictRecord")
            request.sortDescriptors = [NSSortDescriptor(key: "detectedAt", ascending: true)]
            return try Self.requireAllMapped(context.fetch(request), transform: Self.syncConflict(from:))
        }
    }

    func deleteSyncConflict(_ operationId: UUID) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "SyncConflictRecord")
            request.predicate = NSPredicate(format: "operationId == %@", operationId.uuidString)
            if let object = try context.fetch(request).first { context.delete(object) }
        }
    }

    func saveAnalyticsEvent(_ event: AnalyticsEvent) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AnalyticsEventRecord")
            request.predicate = NSPredicate(format: "id == %@", event.id.uuidString)
            guard try context.fetch(request).first == nil else { return }

            let object = NSManagedObject(entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["AnalyticsEventRecord"]!, insertInto: context)
            object.setValue(event.id.uuidString, forKey: "id")
            object.setValue(event.name, forKey: "name")
            object.setValue(event.version, forKey: "version")
            object.setValue((try? JSONEncoder().encode(event.payload)).flatMap { String(data: $0, encoding: .utf8) } ?? "{}", forKey: "payload")
            object.setValue(event.occurredAt, forKey: "occurredAt")
        }
    }

    func fetchPendingAnalyticsEvents() async -> Result<[AnalyticsEvent], PersistenceError> {
        await performRead { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AnalyticsEventRecord")
            request.sortDescriptors = [NSSortDescriptor(key: "occurredAt", ascending: true)]
            request.fetchLimit = 100
            return try Self.requireAllMapped(context.fetch(request), transform: Self.analyticsEvent(from:))
        }
    }

    func deleteAnalyticsEvent(_ id: UUID) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AnalyticsEventRecord")
            request.predicate = NSPredicate(format: "id == %@", id.uuidString)
            if let object = try context.fetch(request).first { context.delete(object) }
        }
    }

    func saveStatistics(_ statistics: WakeStatistics) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "StatisticsRecord")
            request.predicate = NSPredicate(format: "id == %@", "singleton")
            let object: NSManagedObject
            if let existing = try context.fetch(request).first {
                object = existing
            } else {
                object = NSManagedObject(
                    entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["StatisticsRecord"]!,
                    insertInto: context
                )
            }
            object.setValue("singleton", forKey: "id")
            object.setValue(statistics.planned, forKey: "planned")
            object.setValue(statistics.completed, forKey: "completed")
            object.setValue(statistics.snoozes, forKey: "snoozes")
            object.setValue(statistics.fallback, forKey: "fallback")
            object.setValue(statistics.emergencyStops, forKey: "emergencyStops")
            object.setValue(statistics.totalCompletionSeconds, forKey: "totalCompletionSeconds")
        }
    }

    func fetchStatistics() async -> Result<WakeStatistics?, PersistenceError> {
        await performRead { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "StatisticsRecord")
            request.predicate = NSPredicate(format: "id == %@", "singleton")
            guard let object = try context.fetch(request).first else { return nil }
            guard let planned = object.value(forKey: "planned") as? Int,
                  let completed = object.value(forKey: "completed") as? Int,
                  let snoozes = object.value(forKey: "snoozes") as? Int,
                  let fallback = object.value(forKey: "fallback") as? Int,
                  let emergencyStops = object.value(forKey: "emergencyStops") as? Int,
                  let totalCompletionSeconds = object.value(forKey: "totalCompletionSeconds") as? Int else {
                throw Self.invalidRecord()
            }
            return WakeStatistics(
                planned: planned,
                completed: completed,
                snoozes: snoozes,
                fallback: fallback,
                emergencyStops: emergencyStops,
                totalCompletionSeconds: totalCompletionSeconds
            )
        }
    }

    func saveWakeSession(_ session: WakeSession) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "WakeSessionRecord")
            request.predicate = NSPredicate(format: "id == %@", session.id.uuidString)
            let object: NSManagedObject
            if let existing = try context.fetch(request).first {
                object = existing
            } else {
                object = NSManagedObject(
                    entity: context.persistentStoreCoordinator!.managedObjectModel.entitiesByName["WakeSessionRecord"]!,
                    insertInto: context
                )
            }
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
            object.setValue(session.isTest, forKey: "isTest")
        }
    }

    func fetchActiveWakeSession(alarmID: UUID? = nil, isTest: Bool? = nil) async -> Result<WakeSession?, PersistenceError> {
        await performRead { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "WakeSessionRecord")
            var predicates = [NSPredicate(format: "completedAt == nil")]
            if let alarmID {
                predicates.append(NSPredicate(format: "alarmId == %@", alarmID.uuidString))
            }
            if let isTest {
                predicates.append(NSPredicate(format: "isTest == %@", NSNumber(value: isTest)))
            }
            request.predicate = NSCompoundPredicate(andPredicateWithSubpredicates: predicates)
            request.sortDescriptors = [NSSortDescriptor(key: "scheduledAt", ascending: false)]
            guard let object = try context.fetch(request).first else { return nil }
            guard let session = Self.wakeSession(from: object) else { throw Self.invalidRecord() }
            return session
        }
    }

    func fetchWakeSessions(includeTestAlarms: Bool = false, limit: Int = 200) async -> Result<[WakeSession], PersistenceError> {
        await performRead { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "WakeSessionRecord")
            if !includeTestAlarms {
                request.predicate = NSPredicate(format: "isTest == NO")
            }
            request.sortDescriptors = [NSSortDescriptor(key: "scheduledAt", ascending: false)]
            request.fetchLimit = max(1, limit)
            return try Self.requireAllMapped(context.fetch(request), transform: Self.wakeSession(from:))
        }
    }

    func deleteAlarm(_ alarm: Alarm) async -> Bool {
        await performWrite { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.predicate = NSPredicate(format: "id == %@", alarm.id.uuidString)
            if let object = try context.fetch(request).first { context.delete(object) }
        }
    }

    private func performWrite(
        _ work: @escaping @Sendable (NSManagedObjectContext) throws -> Void
    ) async -> Bool {
        guard await waitForPersistentStore() else { return false }
        return await container.performBackgroundTask { context in
            do {
                try work(context)
                if context.hasChanges {
                    try context.save()
                }
                return true
            } catch {
                self.logger.error("Core Data persistence failed: \(error.localizedDescription, privacy: .public)")
                NotificationCenter.default.post(
                    name: .aweroPersistenceSaveFailed,
                    object: nil,
                    userInfo: ["error": error]
                )
                return false
            }
        }
    }

    private func performRead<T: Sendable>(
        _ work: @escaping @Sendable (NSManagedObjectContext) throws -> T
    ) async -> Result<T, PersistenceError> {
        guard await waitForPersistentStore() else { return .failure(.storeUnavailable) }
        return await container.performBackgroundTask { context in
            do {
                return .success(try work(context))
            } catch {
                self.logger.error("Core Data read failed: \(error.localizedDescription, privacy: .public)")
                return .failure(.readFailed(underlying: error))
            }
        }
    }

    private static func requireAllMapped<Source, Destination>(
        _ values: [Source],
        transform: (Source) throws -> Destination?
    ) throws -> [Destination] {
        try values.map { value in
            guard let mapped = try transform(value) else { throw invalidRecord() }
            return mapped
        }
    }

    private static func invalidRecord() -> NSError {
        NSError(
            domain: "app.awero.persistence",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: "A persisted record could not be decoded."]
        )
    }

    private func waitForPersistentStore() async -> Bool {
        do {
            try await persistentStoreReady.value
            return true
        } catch {
            logger.error("Core Data store failed: \(error.localizedDescription, privacy: .public)")
            return false
        }
    }

    private static func syncOperation(from object: NSManagedObject) throws -> SyncOperation? {
        guard let id = UUID(uuidString: object.value(forKey: "id") as? String ?? ""),
              let operationType = object.value(forKey: "operationType") as? String,
              let entityType = object.value(forKey: "entityType") as? String,
              let entityId = object.value(forKey: "entityId") as? String,
              let payloadString = object.value(forKey: "payload") as? String,
              let occurredAt = object.value(forKey: "occurredAt") as? Date else { return nil }
        let payload = try JSONDecoder().decode([String: SyncJSONValue].self, from: Data(payloadString.utf8))
        return SyncOperation(id: id, operationType: operationType, entityType: entityType, entityId: entityId, clientVersion: object.value(forKey: "clientVersion") as? Int, payload: payload, occurredAt: occurredAt)
    }

    private static func syncConflict(from object: NSManagedObject) throws -> SyncConflictRecord? {
        guard let operationId = UUID(uuidString: object.value(forKey: "operationId") as? String ?? ""),
              let localPayloadString = object.value(forKey: "localPayload") as? String,
              let occurredAt = object.value(forKey: "occurredAt") as? Date,
              let detectedAt = object.value(forKey: "detectedAt") as? Date else { return nil }
        let decoder = JSONDecoder()
        let localPayload = try decoder.decode([String: SyncJSONValue].self, from: Data(localPayloadString.utf8))
        let serverEntity: SyncJSONValue?
        if let rawServerEntity = object.value(forKey: "serverEntityJson") as? String {
            serverEntity = try decoder.decode(SyncJSONValue.self, from: Data(rawServerEntity.utf8))
        } else {
            serverEntity = nil
        }
        return SyncConflictRecord(
            operationId: operationId,
            operationType: object.value(forKey: "operationType") as? String ?? "",
            entityType: object.value(forKey: "entityType") as? String ?? "",
            entityId: object.value(forKey: "entityId") as? String ?? "",
            clientVersion: object.value(forKey: "clientVersion") as? Int,
            localPayload: localPayload,
            occurredAt: occurredAt,
            code: object.value(forKey: "code") as? String ?? "",
            serverVersion: object.value(forKey: "serverVersion") as? Int,
            serverEntity: serverEntity,
            detectedAt: detectedAt
        )
    }

    private static func analyticsEvent(from object: NSManagedObject) throws -> AnalyticsEvent? {
        guard let id = UUID(uuidString: object.value(forKey: "id") as? String ?? ""),
              let name = object.value(forKey: "name") as? String,
              let version = object.value(forKey: "version") as? Int,
              let payloadString = object.value(forKey: "payload") as? String,
              let occurredAt = object.value(forKey: "occurredAt") as? Date else { return nil }
        let payload = try JSONDecoder().decode([String: String].self, from: Data(payloadString.utf8))
        return AnalyticsEvent(id: id, name: name, version: version, payload: payload, occurredAt: occurredAt)
    }

    private static func wakeSession(from object: NSManagedObject) -> WakeSession? {
        guard
            let idString = object.value(forKey: "id") as? String,
            let id = UUID(uuidString: idString),
            let alarmIdString = object.value(forKey: "alarmId") as? String,
            let alarmId = UUID(uuidString: alarmIdString),
            let missionRaw = object.value(forKey: "missionType") as? String,
            let mission = MissionType(rawValue: missionRaw),
            let alarmVersion = object.value(forKey: "alarmVersion") as? Int,
            let scheduledAt = object.value(forKey: "scheduledAt") as? Date,
            let snoozeCount = object.value(forKey: "snoozeCount") as? Int,
            let fallbackUsed = object.value(forKey: "fallbackUsed") as? Bool,
            let emergencyStop = object.value(forKey: "emergencyStop") as? Bool,
            let isTest = object.value(forKey: "isTest") as? Bool
        else { return nil }

        return WakeSession(
            id: id, alarmId: alarmId, alarmVersion: alarmVersion,
            scheduledAt: scheduledAt,
            triggeredAt: object.value(forKey: "triggeredAt") as? Date,
            missionStartedAt: object.value(forKey: "missionStartedAt") as? Date,
            completedAt: object.value(forKey: "completedAt") as? Date,
            result: object.value(forKey: "result") as? String, missionType: mission,
            completionTimeSeconds: object.value(forKey: "completionTimeSeconds") as? Int,
            snoozeCount: snoozeCount,
            fallbackUsed: fallbackUsed,
            emergencyStop: emergencyStop,
            isTest: isTest
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
            let difficulty = Difficulty(rawValue: difficultyRaw),
            let version = object.value(forKey: "version") as? Int,
            let hour = object.value(forKey: "hour") as? Int,
            let minute = object.value(forKey: "minute") as? Int,
            let enabled = object.value(forKey: "enabled") as? Bool,
            let weekdays = object.value(forKey: "weekdays") as? String,
            let maxSnoozes = object.value(forKey: "maxSnoozes") as? Int,
            let snoozeMinutes = object.value(forKey: "snoozeMinutes") as? Int
        else { return nil }

        let days = Set(weekdays
            .split(separator: ",")
            .compactMap { Int($0) })

        return Alarm(
            id: id,
            version: version,
            hour: hour,
            minute: minute,
            enabled: enabled,
            weekdays: days,
            timezoneMode: timezone,
            fixedTimezone: object.value(forKey: "fixedTimezone") as? String,
            missionType: mission,
            difficulty: difficulty,
            maxSnoozes: maxSnoozes,
            snoozeMinutes: snoozeMinutes,
            qrExpectedCode: object.value(forKey: "qrExpectedCode") as? String,
            label: object.value(forKey: "label") as? String ?? "Alarm",
            snoozeEnabled: object.value(forKey: "snoozeEnabled") as? Bool ?? true
        )
    }

    static func makeModelForTesting() -> NSManagedObjectModel {
        makeModel()
    }

    private static func makeModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()
        let wakeSessionEntity = entity(name: "WakeSessionRecord", attributes: [
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
            ("emergencyStop", .booleanAttributeType, false),
            ("isTest", .booleanAttributeType, false)
        ])
        wakeSessionEntity.attributesByName["isTest"]?.defaultValue = false
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
                ("qrExpectedCode", .stringAttributeType, true),
                ("label", .stringAttributeType, false),
                ("snoozeEnabled", .booleanAttributeType, false)
            ]),
            entity(name: "SyncOperationRecord", attributes: [
                ("id", .stringAttributeType, false), ("operationType", .stringAttributeType, false), ("entityType", .stringAttributeType, false), ("entityId", .stringAttributeType, false), ("clientVersion", .integer64AttributeType, true), ("payload", .stringAttributeType, false), ("occurredAt", .dateAttributeType, false), ("attempts", .integer64AttributeType, false), ("nextAttemptAt", .dateAttributeType, false)
            ]),
            entity(name: "SyncConflictRecord", attributes: [
                ("operationId", .stringAttributeType, false), ("operationType", .stringAttributeType, false), ("entityType", .stringAttributeType, false), ("entityId", .stringAttributeType, false), ("clientVersion", .integer64AttributeType, true), ("localPayload", .stringAttributeType, false), ("occurredAt", .dateAttributeType, false), ("code", .stringAttributeType, false), ("serverVersion", .integer64AttributeType, true), ("serverEntityJson", .stringAttributeType, true), ("detectedAt", .dateAttributeType, false)
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
            wakeSessionEntity
        ]
        model.entities.first(where: { $0.name == "AlarmRecord" })?.attributesByName["label"]?.defaultValue = "Alarm"
        model.entities.first(where: { $0.name == "AlarmRecord" })?.attributesByName["snoozeEnabled"]?.defaultValue = true
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
