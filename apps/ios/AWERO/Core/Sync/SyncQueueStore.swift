import Foundation

actor SyncQueueStore {
    static let shared = SyncQueueStore()
    private let database = CoreDataStore.shared

    func enqueue(_ operation: SyncOperation) async {
        await database.saveSyncOperation(operation)
    }

    func due() async -> Result<[SyncOperation], PersistenceError> {
        await database.fetchDueSyncOperations()
    }

    func acknowledge(_ id: UUID) async {
        await database.deleteSyncOperation(id)
    }

    func retry(_ id: UUID) async {
        await database.retrySyncOperation(id)
    }

    func recordConflict(_ operation: SyncOperation, conflict: SyncConflict) async -> Bool {
        await database.recordSyncConflict(operation, conflict: conflict)
    }

    func conflicts() async -> Result<[SyncConflictRecord], PersistenceError> {
        await database.fetchSyncConflicts()
    }

    func deleteConflict(_ operationId: UUID) async {
        await database.deleteSyncConflict(operationId)
    }
}
