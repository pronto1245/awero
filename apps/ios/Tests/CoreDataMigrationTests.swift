import XCTest
import CoreData

final class CoreDataMigrationTests: XCTestCase {
    func testLightweightMigrationAddsNewEntitiesAndKeepsAlarm() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let storeURL = directory.appendingPathComponent("AWERO.sqlite")

        let oldModel = CoreDataStore.makeModelForTesting()
        oldModel.entities = oldModel.entities.filter {
            ["AlarmRecord", "StatisticsRecord", "WakeSessionRecord"].contains($0.name)
        }

        let oldContainer = NSPersistentContainer(name: "AWERO", managedObjectModel: oldModel)
        let oldDescription = NSPersistentStoreDescription(url: storeURL)
        oldDescription.shouldMigrateStoreAutomatically = false
        oldDescription.shouldInferMappingModelAutomatically = false
        oldContainer.persistentStoreDescriptions = [oldDescription]

        let loadExpectation = expectation(description: "old store loaded")
        var loadError: Error?
        oldContainer.loadPersistentStores { _, error in
            loadError = error
            loadExpectation.fulfill()
        }
        await fulfillment(of: [loadExpectation], timeout: 5)
        XCTAssertNil(loadError)

        let context = oldContainer.viewContext
        let alarm = NSEntityDescription.insertNewObject(forEntityForName: "AlarmRecord", into: context)
        alarm.setValue(UUID().uuidString, forKey: "id")
        alarm.setValue(1, forKey: "version")
        alarm.setValue(7, forKey: "hour")
        alarm.setValue(30, forKey: "minute")
        alarm.setValue(true, forKey: "enabled")
        alarm.setValue("1,2,3,4,5", forKey: "weekdays")
        alarm.setValue(AlarmTimezoneMode.deviceLocal.rawValue, forKey: "timezoneMode")
        alarm.setValue(nil, forKey: "fixedTimezone")
        alarm.setValue(MissionType.math.rawValue, forKey: "missionType")
        alarm.setValue(Difficulty.medium.rawValue, forKey: "difficulty")
        alarm.setValue(3, forKey: "maxSnoozes")
        alarm.setValue(10, forKey: "snoozeMinutes")
        alarm.setValue(nil, forKey: "qrExpectedCode")
        try context.save()

        let migratedStore = CoreDataStore(storeURL: storeURL)
        let alarms = await migratedStore.fetchAlarms()

        XCTAssertEqual(alarms.count, 1)
        XCTAssertEqual(alarms.first?.hour, 7)
        XCTAssertEqual(alarms.first?.minute, 30)

        try? FileManager.default.removeItem(at: directory)
    }
}
