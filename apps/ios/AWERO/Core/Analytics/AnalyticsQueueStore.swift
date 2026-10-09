import Foundation

struct AnalyticsEvent: Codable, Sendable, Identifiable {
    let id: UUID
    let name: String
    let version: Int
    let payload: [String: String]
    let occurredAt: Date

    init(
        id: UUID = UUID(),
        name: String,
        version: Int = 1,
        payload: [String: String] = [:],
        occurredAt: Date = .now
    ) {
        self.id = id
        self.name = name
        self.version = version
        self.payload = payload
        self.occurredAt = occurredAt
    }
}

actor AnalyticsQueueStore {
    static let shared = AnalyticsQueueStore()
    private let database = CoreDataStore.shared

    func enqueue(_ event: AnalyticsEvent) async {
        await database.saveAnalyticsEvent(event)
    }

    func pending() async -> Result<[AnalyticsEvent], PersistenceError> {
        await database.fetchPendingAnalyticsEvents()
    }

    func acknowledge(_ id: UUID) async {
        await database.deleteAnalyticsEvent(id)
    }
}
