import Foundation
import XCTest
@testable import AWERO

final class SyncAPIClientTests: XCTestCase {
    func testSendsTypedIdempotentBatchAndReturnsConflicts() async throws {
        let operationId = UUID(uuidString: "7d36e5c3-3149-4b28-a985-978a8f230001")!
        let conflictId = UUID(uuidString: "7d36e5c3-3149-4b28-a985-978a8f230002")!
        let transport = StubSyncHTTPTransport(response: SyncHTTPResponse(
            statusCode: 200,
            body: Data("""
            {"acceptedIds":["\(operationId.uuidString)"],"conflicts":[{"id":"\(conflictId.uuidString)","code":"VERSION_MISMATCH","serverVersion":4,"serverEntity":{"hour":9,"enabled":true}}],"serverTime":"2026-10-09T07:00:00Z"}
            """.utf8)
        ))
        let client = SyncAPIClient(
            configuration: SyncAPIConfiguration(value: "https://api.example.test/api/v1/"),
            transport: transport
        )
        let operations = [
            SyncOperation(
                id: operationId,
                operationType: "UPDATE_ALARM",
                entityType: "ALARM",
                entityId: "alarm-1",
                clientVersion: 2,
                payload: ["hour": .integer(7), "enabled": .boolean(true)],
                occurredAt: Date(timeIntervalSince1970: 1_791_523_200)
            )
        ]

        let response = try await client.sync(bearerToken: "test-token", operations: operations)
        let capturedRequest = await transport.request()
        let request = try XCTUnwrap(capturedRequest)
        let body = request.body
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: body) as? [String: Any])
        let sentOperations = try XCTUnwrap(json["operations"] as? [[String: Any]])
        let sent = try XCTUnwrap(sentOperations.first)
        let payload = try XCTUnwrap(sent["payload"] as? [String: Any])

        XCTAssertEqual(request.url.absoluteString, "https://api.example.test/api/v1/sync")
        XCTAssertEqual(request.bearerToken, "test-token")
        XCTAssertEqual(sent["id"] as? String, operationId.uuidString)
        XCTAssertEqual(payload["hour"] as? Int, 7)
        XCTAssertEqual(payload["enabled"] as? Bool, true)
        XCTAssertEqual(sent["clientVersion"] as? Int, 2)
        XCTAssertEqual(response.acceptedIds, [operationId])
        XCTAssertEqual(response.conflicts.first?.id, conflictId)
        XCTAssertEqual(response.conflicts.first?.code, "VERSION_MISMATCH")
        XCTAssertEqual(response.conflicts.first?.serverVersion, 4)
        XCTAssertEqual(response.serverTime, "2026-10-09T07:00:00Z")
    }

    func testUnauthorizedResponseIsNotReportedAsAccepted() async {
        let client = SyncAPIClient(
            configuration: SyncAPIConfiguration(value: "https://api.example.test/api/v1"),
            transport: StubSyncHTTPTransport(response: SyncHTTPResponse(
                statusCode: 401,
                body: Data("{\"message\":\"INVALID_TOKEN\"}".utf8)
            ))
        )

        do {
            _ = try await client.sync(bearerToken: "expired-token", operations: [])
            XCTFail("Expected an authorization error")
        } catch let error as SyncAPIError {
            XCTAssertEqual(error, .httpStatus(401, "INVALID_TOKEN"))
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testMissingEndpointDoesNotCallTransport() async {
        let transport = StubSyncHTTPTransport(response: SyncHTTPResponse(statusCode: 200, body: Data("{}".utf8)))
        let client = SyncAPIClient(configuration: SyncAPIConfiguration(value: nil), transport: transport)

        do {
            _ = try await client.sync(bearerToken: "test-token", operations: [])
            XCTFail("Expected sync to remain disabled")
        } catch let error as SyncAPIError {
            XCTAssertEqual(error, .notConfigured)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
        let request = await transport.request()
        XCTAssertNil(request)
    }
}

private actor StubSyncHTTPTransport: SyncHTTPTransport {
    struct Request: Sendable {
        let url: URL
        let bearerToken: String?
        let body: Data
    }

    private let response: SyncHTTPResponse
    private var lastRequest: Request?

    init(response: SyncHTTPResponse) {
        self.response = response
    }

    func post(url: URL, bearerToken: String?, body: Data) async throws -> SyncHTTPResponse {
        lastRequest = Request(url: url, bearerToken: bearerToken, body: body)
        return response
    }

    func request() -> Request? { lastRequest }
}
