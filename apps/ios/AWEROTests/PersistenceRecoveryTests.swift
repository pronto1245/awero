import XCTest
import CoreData
@testable import AWERO

final class PersistenceRecoveryTests: XCTestCase {
    private func temporaryStoreURL() -> URL {
        FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-(UUID().uuidString)")
            .appendingPathExtension("sqlite")
    }

    func testAlarmAndWakeSessionSurviveStoreRestart() async throws {
        let url = temporaryStoreURL()
        let alarmId = UUID()
        let alarm = Alarm(
            id: alarmId,
            version: 3,
            hour: 7,
            minute: 15,
            enabled: true,
            weekdays: [2, 3, 4, 5, 6],
            timezoneMode: .deviceLocal,
            missionType: .math,
            difficulty: .hard,
            maxSnoozes: 3,
            snoozeMinutes: 10
        )
        let session = WakeSession(
            id: UUID(),
            alarmId: alarm.id,
            alarmVersion: alarm.version,
            scheduledAt: Date(timeIntervalSince1970: 1_000),
            triggeredAt: Date(timeIntervalSince1970: 1_001),
            missionStartedAt: Date(timeIntervalSince1970: 1_010),
            completedAt: nil,
            result: nil,
            missionType: .math,
            completionTimeSeconds: nil,
            snoozeCount: 1,
            fallbackUsed: true,
            emergencyStop: false
        )

        do {
            let first = CoreDataStore(storeURL: url)
            await first.saveAlarm(alarm)
            await first.saveWakeSession(session)
            XCTAssertEqual((await first.fetchAlarm(id: alarm.id))?.version, 3)
            XCTAssertEqual((await first.fetchActiveWakeSession())?.id, session.id)
        }

        let second = CoreDataStore(storeURL: url)
        XCTAssertEqual((await second.fetchAlarm(id: alarm.id))?.id, alarm.id)
        let restored = await second.fetchActiveWakeSession()
        XCTAssertEqual(restored?.id, session.id)
        XCTAssertEqual(restored?.snoozeCount, 1)
        XCTAssertTrue(restored?.fallbackUsed == true)

        try? FileManager.default.removeItem(at: url)
        try? FileManager.default.removeItem(at: URL(fileURLWithPath: url.path + "-shm"))
        try? FileManager.default.removeItem(at: URL(fileURLWithPath: url.path + "-wal"))
    }

    func testSyncOperationDuplicateIdIsStoredOnce() async throws {
        let url = temporaryStoreURL()
        let id = UUID()
        let operation = SyncOperation(
            id: id,
            operationType: "CREATE_ALARM",
            entityType: "alarm",
            entityId: "alarm-1",
            clientVersion: 1,
            payload: ["hour": "7"]
        )

        let store = CoreDataStore(storeURL: url)
        await store.saveSyncOperation(operation)
        await store.saveSyncOperation(operation)
        XCTAssertEqual((await store.fetchDueSyncOperations()).filter { $0.id == id }.count, 1)

        try? FileManager.default.removeItem(at: url)
        try? FileManager.default.removeItem(at: URL(fileURLWithPath: url.path + "-shm"))
        try? FileManager.default.removeItem(at: URL(fileURLWithPath: url.path + "-wal"))
    }
}
