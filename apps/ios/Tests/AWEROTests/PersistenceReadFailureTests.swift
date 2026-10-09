import Foundation
import XCTest
@testable import AWERO

final class PersistenceReadFailureTests: XCTestCase {
    @MainActor
    func testUnavailableStoreIsNeverReportedAsAnEmptyCollectionOrMissingRecord() async throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-read-failure-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let storeURL = directory.appendingPathComponent("AWERO.sqlite")
        try Data("not a SQLite database".utf8).write(to: storeURL)
        let database = CoreDataStore(storeURL: storeURL)

        assertReadFailure(await database.fetchAlarm(id: UUID()))
        assertReadFailure(await database.fetchAlarms())
        assertReadFailure(await database.fetchDueSyncOperations())
        assertReadFailure(await database.fetchSyncConflicts())
        assertReadFailure(await database.fetchPendingAnalyticsEvents())
        assertReadFailure(await database.fetchStatistics())
        assertReadFailure(await database.fetchActiveWakeSession())
        assertReadFailure(await database.fetchWakeSessions())

        let alarmStore = AlarmStore(database: database)
        await alarmStore.load()
        XCTAssertEqual(alarmStore.loadState, .failed)
    }

    func testGenuinelyEmptyStoreReturnsSuccessWithNoRecords() async throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-empty-store-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let database = CoreDataStore(storeURL: directory.appendingPathComponent("AWERO.sqlite"))
        let alarms = try await database.fetchAlarms().get()
        let sessions = try await database.fetchWakeSessions().get()
        let active = try await database.fetchActiveWakeSession().get()
        XCTAssertTrue(alarms.isEmpty)
        XCTAssertTrue(sessions.isEmpty)
        XCTAssertNil(active)
    }

    @MainActor
    func testFailedLegacyAlarmMigrationIsNotMarkedCompleteAndCanBeRetried() async throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-alarm-migration-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let suiteName = "awero-tests-\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        defaults.set(Data("invalid legacy alarms".utf8), forKey: "awero.alarms.v1")

        let store = AlarmStore(database: CoreDataStore(storeURL: directory.appendingPathComponent("AWERO.sqlite")), defaults: defaults)
        await store.load()
        XCTAssertEqual(store.loadState, .failed)
        XCTAssertFalse(defaults.bool(forKey: "awero.coredata.migrated.v1"))

        let expected = Alarm(hour: 7, minute: 15)
        defaults.set(try JSONEncoder().encode([expected]), forKey: "awero.alarms.v1")
        await store.load()
        XCTAssertEqual(store.loadState, .loaded)
        XCTAssertTrue(defaults.bool(forKey: "awero.coredata.migrated.v1"))
        XCTAssertEqual(store.alarms.map(\.id), [expected.id])
    }

    @MainActor
    func testCompletedStatisticsMigrationDoesNotReimportStaleLegacySnapshot() async throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-statistics-migration-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let suiteName = "awero-statistics-tests-\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        var staleStatistics = WakeStatistics()
        staleStatistics.planned = 99
        defaults.set(try JSONEncoder().encode(staleStatistics), forKey: "awero.statistics.v1")
        defaults.set(true, forKey: "awero.coredata.statistics.migrated.v1")

        let database = CoreDataStore(storeURL: directory.appendingPathComponent("AWERO.sqlite"))
        let store = StatisticsStore(database: database, defaults: defaults)
        await store.load()

        XCTAssertFalse(store.loadFailed)
        XCTAssertEqual(store.statistics.planned, 0)
        let persisted = try await database.fetchStatistics().get()
        XCTAssertNil(persisted)
    }

    private func assertReadFailure<Value>(
        _ result: Result<Value, PersistenceError>,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard case .failure = result else {
            XCTFail("A persistence read failure must remain distinct from empty data.", file: file, line: line)
            return
        }
    }
}
