import Foundation

struct SyncOperation: Codable, Sendable, Identifiable {
    let id: UUID
    let operationType: String
    let entityType: String
    let entityId: String
    let clientVersion: Int?
    let payload: [String: String]
    let occurredAt: Date

    init(
        id: UUID = UUID(),
        operationType: String,
        entityType: String,
        entityId: String,
        clientVersion: Int? = nil,
        payload: [String: String] = [:],
        occurredAt: Date = .now
    ) {
        self.id = id
        self.operationType = operationType
        self.entityType = entityType
        self.entityId = entityId
        self.clientVersion = clientVersion
        self.payload = payload
        self.occurredAt = occurredAt
    }
}
