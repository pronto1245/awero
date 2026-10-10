import Foundation

struct SupportDiagnosticsAPIClient {
    private let configuration: SyncAPIConfiguration
    private let transport: any SyncHTTPTransport

    init(configuration: SyncAPIConfiguration, transport: any SyncHTTPTransport = URLSessionSyncHTTPTransport()) {
        self.configuration = configuration
        self.transport = transport
    }

    func reportSyncServerFailure(statusCode: Int, bearerToken: String, installationID: String, now: Date = .now) async {
        guard configuration.isEnabled, let installationUUID = UUID(uuidString: installationID),
              let url = configuration.endpoint("support/diagnostics") else { return }
        let day = ISO8601DateFormatter().string(from: Calendar(identifier: .gregorian).startOfDay(for: now))
        let ticketID = Self.stableID(installationUUID, suffix: "sync-server-failure-\(day)")
        let diagnostics: [String: String] = [
            "platform": "IOS",
            "appVersion": Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown",
            "osVersion": ProcessInfo.processInfo.operatingSystemVersionString,
            "timezone": TimeZone.current.identifier,
            "errorCode": "SYNC_HTTP_\(statusCode)"
        ]
        guard let body = try? JSONSerialization.data(withJSONObject: [
            "id": ticketID,
            "category": "SYNC",
            "diagnostics": diagnostics
        ]) else { return }
        _ = try? await transport.post(url: url, bearerToken: bearerToken, body: body)
    }

    private static func stableID(_ namespace: UUID, suffix: String) -> String {
        var bytes = withUnsafeBytes(of: namespace.uuid) { Array($0) }
        var hash: UInt64 = 14_695_981_039_346_656_037
        for byte in suffix.utf8 { hash = (hash ^ UInt64(byte)) &* 1_099_511_628_211 }
        for offset in 0..<8 { bytes[8 + offset] = UInt8(truncatingIfNeeded: hash >> (offset * 8)) }
        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
                           bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15])).uuidString
    }
}
