import Foundation

struct SyncHTTPResponse: Sendable {
    let statusCode: Int
    let body: Data
}

protocol SyncHTTPTransport: Sendable {
    func post(url: URL, bearerToken: String?, body: Data) async throws -> SyncHTTPResponse
}

struct SyncConflict: Decodable, Sendable {
    let id: UUID
    let code: String
    let serverVersion: Int?
    let serverEntity: SyncJSONValue?
}

struct SyncBatchResponse: Decodable, Sendable {
    let acceptedIds: [UUID]
    let conflicts: [SyncConflict]
    let serverTime: String?
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
}

private struct SyncBatchRequest: Encodable {
    let operations: [SyncOperation]
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
}
