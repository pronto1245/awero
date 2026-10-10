import Foundation

struct AnonymousRegistrationRequest: Encodable, Sendable {
    let deviceId: String
    let installationSecret: String
    let platform: String
    let appVersion: String
    let osVersion: String?
    let timezone: String

    init(deviceId: String, installationSecret: String, platform: String, appVersion: String, osVersion: String? = nil, timezone: String) throws {
        guard (16...128).contains(deviceId.count) else {
            throw SyncAPIError.invalidRequest("Device ID must contain 16 to 128 characters")
        }
        guard installationSecret.range(of: "^[A-Za-z0-9_-]{43}$", options: .regularExpression) != nil else {
            throw SyncAPIError.invalidRequest("Installation secret is invalid")
        }
        guard platform == "IOS" || platform == "ANDROID" else {
            throw SyncAPIError.invalidRequest("Unsupported platform")
        }
        guard (1...64).contains(appVersion.count), (osVersion?.count ?? 0) <= 64,
              (1...64).contains(timezone.count) else {
            throw SyncAPIError.invalidRequest("Registration fields are outside the supported lengths")
        }
        self.deviceId = deviceId
        self.installationSecret = installationSecret
        self.platform = platform
        self.appVersion = appVersion
        self.osVersion = osVersion
        self.timezone = timezone
    }
}

struct AnonymousRegistrationResponse: Decodable, Sendable {
    let anonymousUserId: UUID
    let deviceId: UUID
    let accessToken: String
    let tokenType: String
    let expiresInDays: Int
    let authMode: String
    let syncEnabled: Bool
}

struct AnonymousAuthAPIClient {
    private let configuration: SyncAPIConfiguration
    private let transport: any SyncHTTPTransport

    init(configuration: SyncAPIConfiguration, transport: any SyncHTTPTransport = URLSessionSyncHTTPTransport()) {
        self.configuration = configuration
        self.transport = transport
    }

    func register(_ registration: AnonymousRegistrationRequest) async throws -> AnonymousRegistrationResponse {
        guard let url = configuration.endpoint("auth/anonymous") else { throw SyncAPIError.notConfigured }
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        let response = try await transport.post(url: url, bearerToken: nil, body: encoder.encode(registration))
        guard (200..<300).contains(response.statusCode) else {
            let message = (try? JSONDecoder().decode(SyncErrorBody.self, from: response.body).message)
            throw SyncAPIError.httpStatus(response.statusCode, message)
        }
        let result: AnonymousRegistrationResponse
        do {
            result = try JSONDecoder().decode(AnonymousRegistrationResponse.self, from: response.body)
        } catch {
            throw SyncAPIError.invalidResponse
        }
        guard !result.accessToken.isEmpty, result.tokenType.caseInsensitiveCompare("Bearer") == .orderedSame,
              result.expiresInDays > 0, result.syncEnabled, result.authMode == "anonymous" else {
            throw SyncAPIError.invalidResponse
        }
        return result
    }

    func bindInstallation(_ installationSecret: String, bearerToken: String) async throws {
        guard let url = configuration.endpoint("auth/anonymous/credentials") else { throw SyncAPIError.notConfigured }
        let body = try JSONSerialization.data(withJSONObject: ["installationSecret": installationSecret])
        let response = try await transport.post(url: url, bearerToken: bearerToken, body: body)
        guard (200..<300).contains(response.statusCode) else {
            let message = (try? JSONDecoder().decode(SyncErrorBody.self, from: response.body).message)
            throw SyncAPIError.httpStatus(response.statusCode, message)
        }
    }
}
