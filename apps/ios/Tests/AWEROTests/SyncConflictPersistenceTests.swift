import Foundation
import XCTest
@testable import AWERO

final class SyncConflictPersistenceTests: XCTestCase {
    func testConflictIsPersistedAndRemovedFromRetryQueue() async throws {
        let storeURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-sync-conflict-\(UUID().uuidString).sqlite")
        let store = CoreDataStore(storeURL: storeURL)
        let operationId = UUID()
        let operation = SyncOperation(
            id: operationId,
            operationType: "UPDATE_ALARM",
            entityType: "ALARM",
            entityId: "alarm-1",
            clientVersion: 1,
            payload: ["hour": .integer(8)],
            occurredAt: Date(timeIntervalSince1970: 1_700_000_000)
        )
        let conflict = SyncConflict(
            id: operationId,
            code: "VERSION_MISMATCH",
            serverVersion: 2,
            serverEntity: .object(["hour": .integer(9)])
        )

        let operationSaved = await store.saveSyncOperation(operation)
        XCTAssertTrue(operationSaved)
        let conflictSaved = await store.recordSyncConflict(operation, conflict: conflict)
        XCTAssertTrue(conflictSaved)

        let queued = await store.fetchDueSyncOperations()
        let conflicts = await store.fetchSyncConflicts()
        XCTAssertTrue(queued.isEmpty)
        XCTAssertEqual(conflicts.count, 1)
        XCTAssertEqual(conflicts[0].operationId, operationId)
        XCTAssertEqual(conflicts[0].code, "VERSION_MISMATCH")
        XCTAssertEqual(conflicts[0].serverVersion, 2)
        XCTAssertEqual(conflicts[0].localPayload["hour"], .integer(8))
        XCTAssertEqual(conflicts[0].serverEntity, .object(["hour": .integer(9)]))
    }
}
