import Foundation

enum SyncJSONValue: Codable, Sendable, Equatable {
    case object([String: SyncJSONValue])
    case array([SyncJSONValue])
    case string(String)
    case integer(Int64)
    case number(Double)
    case boolean(Bool)
    case null

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .boolean(value)
        } else if let value = try? container.decode(Int64.self) {
            self = .integer(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([String: SyncJSONValue].self) {
            self = .object(value)
        } else if let value = try? container.decode([SyncJSONValue].self) {
            self = .array(value)
        } else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported JSON value")
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .object(let value): try container.encode(value)
        case .array(let value): try container.encode(value)
        case .string(let value): try container.encode(value)
        case .integer(let value): try container.encode(value)
        case .number(let value): try container.encode(value)
        case .boolean(let value): try container.encode(value)
        case .null: try container.encodeNil()
        }
    }
}

struct SyncOperation: Codable, Sendable, Identifiable {
    let id: UUID
    let operationType: String
    let entityType: String
    let entityId: String
    let clientVersion: Int?
    let payload: [String: SyncJSONValue]
    let occurredAt: Date

    init(
        id: UUID = UUID(),
        operationType: String,
        entityType: String,
        entityId: String,
        clientVersion: Int? = nil,
        payload: [String: SyncJSONValue] = [:],
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

    init(
        id: UUID = UUID(),
        operationType: String,
        entityType: String,
        entityId: String,
        clientVersion: Int? = nil,
        payload: [String: String],
        occurredAt: Date = .now
    ) {
        self.init(
            id: id,
            operationType: operationType,
            entityType: entityType,
            entityId: entityId,
            clientVersion: clientVersion,
            payload: payload.mapValues(SyncJSONValue.string),
            occurredAt: occurredAt
        )
    }
}

struct SyncConflictRecord: Sendable, Identifiable {
    let operationId: UUID
    let operationType: String
    let entityType: String
    let entityId: String
    let clientVersion: Int?
    let localPayload: [String: SyncJSONValue]
    let occurredAt: Date
    let code: String
    let serverVersion: Int?
    let serverEntity: SyncJSONValue?
    let detectedAt: Date

    var id: UUID { operationId }
}
