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

    func retry(_ id: UUID, attempts: Int) async {
        let exponent = min(max(attempts, 0), 6)
        let delay = Double(1 << exponent)
        await database.retrySyncOperation(id, nextAttemptAt: .now.addingTimeInterval(delay))
    }
}