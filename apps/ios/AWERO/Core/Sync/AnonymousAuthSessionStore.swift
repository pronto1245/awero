import Foundation
import Security

struct AnonymousAuthSession: Codable, Sendable {
    let anonymousUserId: UUID
    let serverDeviceId: UUID
    let accessToken: String
    let expiresAt: Date
}

enum AnonymousAuthSessionStoreError: Error {
    case keychain(OSStatus)
}

struct AnonymousAuthSessionStore {
    private let service = "app.awero.anonymous-auth"
    private let sessionAccount = "session"
    private let installationAccount = "installation-id"

    func installationID() throws -> String {
        if let data = try read(account: installationAccount),
           let value = String(data: data, encoding: .utf8), !value.isEmpty {
            return value
        }
        let value = UUID().uuidString
        try write(Data(value.utf8), account: installationAccount)
        return value
    }

    func loadSession() throws -> AnonymousAuthSession? {
        guard let data = try read(account: sessionAccount) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        do {
            return try decoder.decode(AnonymousAuthSession.self, from: data)
        } catch {
            try delete(account: sessionAccount)
            return nil
        }
    }

    func saveSession(_ session: AnonymousAuthSession) throws {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        try write(encoder.encode(session), account: sessionAccount)
    }

    func clearSession() throws {
        try delete(account: sessionAccount)
    }

    private func read(account: String) throws -> Data? {
        var query = baseQuery(account: account)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess, let data = result as? Data else {
            throw AnonymousAuthSessionStoreError.keychain(status)
        }
        return data
    }

    private func write(_ data: Data, account: String) throws {
        let query = baseQuery(account: account)
        let attributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var insertion = query
            attributes.forEach { insertion[$0.key] = $0.value }
            let addStatus = SecItemAdd(insertion as CFDictionary, nil)
            guard addStatus == errSecSuccess else { throw AnonymousAuthSessionStoreError.keychain(addStatus) }
        } else if status != errSecSuccess {
            throw AnonymousAuthSessionStoreError.keychain(status)
        }
    }

    private func baseQuery(account: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }

    private func delete(account: String) throws {
        let status = SecItemDelete(baseQuery(account: account) as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw AnonymousAuthSessionStoreError.keychain(status)
        }
    }
}

struct AnonymousAuthCoordinator {
    private let configuration: SyncAPIConfiguration
    private let store: AnonymousAuthSessionStore

    init(configuration: SyncAPIConfiguration = SyncAPIConfiguration(), store: AnonymousAuthSessionStore = AnonymousAuthSessionStore()) {
        self.configuration = configuration
        self.store = store
    }

    func refreshIfNeeded(now: Date = .now) async {
        guard configuration.isEnabled else { return }
        do {
            if let session = try store.loadSession(), session.expiresAt.timeIntervalSince(now) > 5 * 24 * 60 * 60 {
                return
            }
            let registration = try AnonymousRegistrationRequest(
                deviceId: store.installationID(),
                platform: "IOS",
                appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.1.0",
                osVersion: ProcessInfo.processInfo.operatingSystemVersionString,
                timezone: TimeZone.current.identifier
            )
            let response = try await AnonymousAuthAPIClient(configuration: configuration).register(registration)
            let expiresAt = Calendar(identifier: .gregorian).date(byAdding: .day, value: response.expiresInDays, to: now) ?? now
            try store.saveSession(AnonymousAuthSession(
                anonymousUserId: response.anonymousUserId,
                serverDeviceId: response.deviceId,
                accessToken: response.accessToken,
                expiresAt: expiresAt
            ))
        } catch {
            // Auth and sync are optional and must not affect local alarm behavior.
        }
    }
}
