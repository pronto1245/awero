import Foundation

struct SyncHTTPResponse: Sendable {
    let statusCode: Int
    let body: Data
}

protocol SyncHTTPTransport: Sendable {
    func post(url: URL, bearerToken: String?, body: Data) async throws -> SyncHTTPResponse
    func get(url: URL, bearerToken: String) async throws -> SyncHTTPResponse
}

extension SyncHTTPTransport {
    func get(url: URL, bearerToken: String) async throws -> SyncHTTPResponse {
        throw SyncAPIError.invalidRequest("HTTP GET transport is unavailable")
    }
}

struct SyncConflict: Decodable, Sendable {
    let id: UUID
    let code: String
    let serverVersion: Int?
    let serverEntity: SyncJSONValue?
}

struct SyncRejection: Decodable, Sendable {
    let id: UUID
    let code: String
}

struct SyncBatchResponse: Decodable, Sendable {
    let acceptedIds: [UUID]
    let conflicts: [SyncConflict]
    let rejected: [SyncRejection]
    let serverTime: String?

    private enum CodingKeys: String, CodingKey { case acceptedIds, conflicts, rejected, serverTime }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        acceptedIds = try container.decode([UUID].self, forKey: .acceptedIds)
        conflicts = try container.decode([SyncConflict].self, forKey: .conflicts)
        rejected = try container.decodeIfPresent([SyncRejection].self, forKey: .rejected) ?? []
        serverTime = try container.decodeIfPresent(String.self, forKey: .serverTime)
    }
}

struct AnalyticsBatchResponse: Decodable, Sendable {
    let acceptedIds: [UUID]
    let rejected: [SyncRejection]
}

enum SyncAPIError: Error, Equatable {
    case notConfigured
    case invalidResponse
    case invalidRequest(String)
    case httpStatus(Int, String?)
}

struct SyncAPIClient {
    private let configuration: SyncAPIConfiguration
    private let transport: any SyncHTTPTransport

    init(configuration: SyncAPIConfiguration, transport: any SyncHTTPTransport = URLSessionSyncHTTPTransport()) {
        self.configuration = configuration
        self.transport = transport
    }

    func sync(bearerToken: String, operations: [SyncOperation]) async throws -> SyncBatchResponse {
        guard !bearerToken.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw SyncAPIError.invalidRequest("A bearer token is required")
        }
        guard operations.count <= 100 else {
            throw SyncAPIError.invalidRequest("Sync batches are limited to 100 operations")
        }
        guard let url = configuration.endpoint("sync") else { throw SyncAPIError.notConfigured }

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.sortedKeys]
        let body = try encoder.encode(SyncBatchRequest(operations: operations))
        let response = try await transport.post(url: url, bearerToken: bearerToken, body: body)
        guard (200..<300).contains(response.statusCode) else {
            let message = (try? JSONDecoder().decode(SyncErrorBody.self, from: response.body).message)
            throw SyncAPIError.httpStatus(response.statusCode, message)
        }
        do {
            return try JSONDecoder().decode(SyncBatchResponse.self, from: response.body)
        } catch {
            throw SyncAPIError.invalidResponse
        }
    }

    func sendAnalytics(bearerToken: String, events: [AnalyticsEvent]) async throws -> AnalyticsBatchResponse {
        guard !bearerToken.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw SyncAPIError.invalidRequest("A bearer token is required")
        }
        guard events.count <= 100 else { throw SyncAPIError.invalidRequest("Analytics batches are limited to 100 events") }
        guard let url = configuration.endpoint("analytics/events") else { throw SyncAPIError.notConfigured }
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.sortedKeys]
        let body = try encoder.encode(AnalyticsBatchRequest(events: events.map {
            AnalyticsEventRequest(id: $0.id, eventName: $0.name, eventVersion: $0.version, properties: $0.payload, occurredAt: $0.occurredAt)
        }))
        let response = try await transport.post(url: url, bearerToken: bearerToken, body: body)
        guard (200..<300).contains(response.statusCode) else {
            let message = (try? JSONDecoder().decode(SyncErrorBody.self, from: response.body).message)
            throw SyncAPIError.httpStatus(response.statusCode, message)
        }
        do { return try JSONDecoder().decode(AnalyticsBatchResponse.self, from: response.body) }
        catch { throw SyncAPIError.invalidResponse }
    }
}

private struct SyncBatchRequest: Encodable {
    let operations: [SyncOperation]
}

private struct AnalyticsBatchRequest: Encodable { let events: [AnalyticsEventRequest] }
private struct AnalyticsEventRequest: Encodable {
    let id: UUID
    let eventName: String
    let eventVersion: Int
    let properties: [String: String]
    let occurredAt: Date
}

struct SyncErrorBody: Decodable {
    let message: String?
}

private final class RejectRedirectsDelegate: NSObject, URLSessionTaskDelegate {
    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest request: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        completionHandler(nil)
    }
}

final class URLSessionSyncHTTPTransport: SyncHTTPTransport, @unchecked Sendable {
    private let redirectDelegate: RejectRedirectsDelegate
    private let session: URLSession

    init() {
        let delegate = RejectRedirectsDelegate()
        redirectDelegate = delegate
        session = URLSession(configuration: .ephemeral, delegate: delegate, delegateQueue: nil)
    }

    func post(url: URL, bearerToken: String?, body: Data) async throws -> SyncHTTPResponse {
        guard url.scheme?.lowercased() == "https" else { throw SyncAPIError.notConfigured }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.httpBody = body
        request.httpShouldHandleCookies = false
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        if let bearerToken {
            request.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        }
        request.timeoutInterval = 15

        let (data, response) = try await session.data(for: request)
        guard let response = response as? HTTPURLResponse else { throw SyncAPIError.invalidResponse }
        return SyncHTTPResponse(statusCode: response.statusCode, body: data)
    }

    func get(url: URL, bearerToken: String) async throws -> SyncHTTPResponse {
        guard url.scheme?.lowercased() == "https" else { throw SyncAPIError.notConfigured }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.httpShouldHandleCookies = false
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 15
        let (data, response) = try await session.data(for: request)
        guard let response = response as? HTTPURLResponse else { throw SyncAPIError.invalidResponse }
        return SyncHTTPResponse(statusCode: response.statusCode, body: data)
    }
}
