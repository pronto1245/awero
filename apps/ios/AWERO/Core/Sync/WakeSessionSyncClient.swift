import Foundation

struct WakeSessionSyncClient {
    private let configuration: SyncAPIConfiguration
    private let transport: any SyncHTTPTransport
    private let database: CoreDataStore

    init(
        configuration: SyncAPIConfiguration = SyncAPIConfiguration(),
        transport: any SyncHTTPTransport = URLSessionSyncHTTPTransport(),
        database: CoreDataStore = .shared
    ) {
        self.configuration = configuration
        self.transport = transport
        self.database = database
    }

    func uploadRecent(bearerToken: String, now: Date = .now) async {
        guard configuration.isEnabled,
              case let .success(sessions) = await database.fetchWakeSessions(includeTestAlarms: false) else { return }
        let oldestAccepted = now.addingTimeInterval(-30 * 24 * 60 * 60)
        for session in sessions where session.triggeredAt.map({ $0 >= oldestAccepted && $0 <= now.addingTimeInterval(5 * 60) }) == true {
            do {
                try await upload(session, bearerToken: bearerToken)
            } catch {
                // The session remains in Core Data and the idempotent upload is retried on the next sync pass.
                return
            }
        }
    }

    private func upload(_ session: WakeSession, bearerToken: String) async throws {
        guard let triggeredAt = session.triggeredAt,
              let createURL = configuration.endpoint("wake-sessions") else { return }
        let createBody: [String: Any] = [
            "id": session.id.uuidString,
            "alarmId": session.alarmId.uuidString,
            "alarmVersion": session.alarmVersion,
            "scheduledAt": Self.timestamp(session.scheduledAt),
            "triggeredAt": Self.timestamp(triggeredAt),
            "missionType": session.missionType.rawValue,
            "eventId": session.id.uuidString
        ]
        try await post(createURL, body: createBody, bearerToken: bearerToken)

        if session.snoozeCount > 0 {
            try await append(session, type: "SNOOZE", suffix: "snooze-\(session.snoozeCount)", at: triggeredAt,
                             payload: ["count": session.snoozeCount], bearerToken: bearerToken)
        }
        if let missionStartedAt = session.missionStartedAt {
            try await append(session, type: "MISSION_STARTED", suffix: "mission-started", at: missionStartedAt,
                             payload: [:], bearerToken: bearerToken)
            if session.fallbackUsed {
                try await append(session, type: "FALLBACK", suffix: "fallback", at: missionStartedAt,
                                 payload: [:], bearerToken: bearerToken)
            }
        }
        guard let completedAt = session.completedAt else { return }
        let eventType: String
        switch session.result {
        case "COMPLETED", "SUCCESS": eventType = "COMPLETED"
        case "EMERGENCY_STOP": eventType = "EMERGENCY_STOP"
        case "CANCELLED": eventType = "CANCELLED"
        default: return
        }
        try await append(session, type: eventType, suffix: eventType.lowercased(), at: completedAt,
                         payload: [:], bearerToken: bearerToken)
    }

    private func append(
        _ session: WakeSession,
        type: String,
        suffix: String,
        at: Date,
        payload: [String: Any],
        bearerToken: String
    ) async throws {
        guard let url = configuration.endpoint("wake-sessions/\(session.id.uuidString)/events") else { return }
        let body: [String: Any] = [
            "id": Self.stableEventID(session.id, suffix: suffix),
            "eventType": type,
            "occurredAt": Self.timestamp(at),
            "payload": payload
        ]
        try await post(url, body: body, bearerToken: bearerToken)
    }

    private func post(_ url: URL, body: [String: Any], bearerToken: String) async throws {
        let data = try JSONSerialization.data(withJSONObject: body, options: [.sortedKeys])
        let response = try await transport.post(url: url, bearerToken: bearerToken, body: data)
        guard (200..<300).contains(response.statusCode) else {
            let message = (try? JSONDecoder().decode(SyncErrorBody.self, from: response.body).message)
            throw SyncAPIError.httpStatus(response.statusCode, message)
        }
    }

    private static func timestamp(_ date: Date) -> String {
        ISO8601DateFormatter().string(from: date)
    }

    private static func stableEventID(_ sessionID: UUID, suffix: String) -> String {
        var bytes = withUnsafeBytes(of: sessionID.uuid) { Array($0) }
        var hash: UInt64 = 14_695_981_039_346_656_037
        for byte in suffix.utf8 {
            hash = (hash ^ UInt64(byte)) &* 1_099_511_628_211
        }
        for offset in 0..<8 { bytes[8 + offset] = UInt8(truncatingIfNeeded: hash >> (offset * 8)) }
        bytes[8] = (bytes[8] & 0x3f) | 0x80
        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
                           bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15])).uuidString
    }
}
