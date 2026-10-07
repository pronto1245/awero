import Foundation

actor SyncQueueStore {
    static let shared = SyncQueueStore()
    private let database = CoreDataStore.shared

    func enqueue(_ operation: SyncOperation) async {
        await database.saveSyncOperation(operation)
    }

    func due() async -> [SyncOperation] {
        await database.fetchDueSyncOperations()
    }

    func acknowledge(_ id: UUID) async {
        await database.deleteSyncOperation(id)
    }
}
