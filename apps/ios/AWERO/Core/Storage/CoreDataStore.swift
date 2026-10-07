import CoreData
import Foundation

final class CoreDataStore {
    static let shared = CoreDataStore()

    private let container: NSPersistentContainer

    private init() {
        let model = Self.makeModel()
        container = NSPersistentContainer(name: "AWERO", managedObjectModel: model)

        let storeURL = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("AWERO.sqlite")
        try? FileManager.default.createDirectory(
            at: storeURL.deletingLastPathComponent(),
            withIntermediateDirectories: true
        )

        let description = NSPersistentStoreDescription(url: storeURL)
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
                entity: self.entity(named: "AlarmRecord"),
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

    func deleteAlarm(_ alarm: Alarm) async {
        await performBackground { context in
            let request = NSFetchRequest<NSManagedObject>(entityName: "AlarmRecord")
            request.predicate = NSPredicate(format: "id == %@", alarm.id.uuidString)
            if let object = try? context.fetch(request).first {
                context.delete(object)
            }
        }
    }

    private func performBackground<T: Sendable>(
        _ work: @escaping @Sendable (NSManagedObjectContext) -> T
    ) async -> T {
        await container.performBackgroundTask { context in
            let result = work(context)
            if context.hasChanges { try? context.save() }
            return result
        }
    }

    private func entity(named name: String) -> NSEntityDescription {
        container.managedObjectModel.entitiesByName[name]!
    }

    private static func alarm(from object: NSManagedObject) -> Alarm? {
        guard
            let idString = object.value(forKey: "id") as? String,
            let id = UUID(uuidString: idString),
            let timezoneRaw = object.value(forKey: "timezoneMode") as? String,
            let timezone = TimezoneMode(rawValue: timezoneRaw),
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

    private static func makeModel() -> NSManagedObjectModel {
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
