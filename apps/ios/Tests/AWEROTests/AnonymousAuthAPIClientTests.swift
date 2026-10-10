import Foundation
import XCTest
@testable import AWERO

final class AnonymousAuthAPIClientTests: XCTestCase {
    func testRegistersWithoutBearerAndDecodesIssuedToken() async throws {
        let userId = UUID(uuidString: "a8f8f3a0-1518-4f47-9cb3-50e6b6000001")!
        let deviceId = UUID(uuidString: "a8f8f3a0-1518-4f47-9cb3-50e6b6000002")!
        let transport = AnonymousAuthStubTransport(response: SyncHTTPResponse(
            statusCode: 201,
            body: Data("""
            {"anonymousUserId":"\(userId)","deviceId":"\(deviceId)","accessToken":"secret","tokenType":"Bearer","expiresInDays":30,"authMode":"anonymous","syncEnabled":true}
            """.utf8)
        ))
        let client = AnonymousAuthAPIClient(
            configuration: SyncAPIConfiguration(value: "https://api.example.test/api/v1/"),
            transport: transport
        )
        let registration = try AnonymousRegistrationRequest(
            deviceId: "device-installation-1234", installationSecret: String(repeating: "a", count: 43), platform: "IOS", appVersion: "0.1.0",
            osVersion: "18.0", timezone: "Europe/Moscow"
        )

        let result = try await client.register(registration)
        let capturedRequest = await transport.request()
        let request = try XCTUnwrap(capturedRequest)
        let body = try XCTUnwrap(JSONSerialization.jsonObject(with: request.body) as? [String: String])

        XCTAssertEqual(request.url.absoluteString, "https://api.example.test/api/v1/auth/anonymous")
        XCTAssertNil(request.bearerToken)
        XCTAssertEqual(body["deviceId"], "device-installation-1234")
        XCTAssertEqual(body["installationSecret"], String(repeating: "a", count: 43))
        XCTAssertEqual(body["platform"], "IOS")
        XCTAssertEqual(body["osVersion"], "18.0")
        XCTAssertEqual(body["timezone"], "Europe/Moscow")
        XCTAssertEqual(result.anonymousUserId, userId)
        XCTAssertEqual(result.deviceId, deviceId)
        XCTAssertEqual(result.accessToken, "secret")
        XCTAssertEqual(result.expiresInDays, 30)
        XCTAssertTrue(result.syncEnabled)
    }

    func testRejectsInvalidRegistrationAndReportsHTTPFailure() async throws {
        XCTAssertThrowsError(try AnonymousRegistrationRequest(
            deviceId: "short", installationSecret: "invalid", platform: "IOS", appVersion: "0.1.0", timezone: "UTC"
        ))
        let client = AnonymousAuthAPIClient(
            configuration: SyncAPIConfiguration(value: "https://api.example.test/api/v1"),
            transport: AnonymousAuthStubTransport(response: SyncHTTPResponse(
                statusCode: 400, body: Data("{\"message\":\"INVALID_REQUEST\"}".utf8)
            ))
        )
        let registration = try AnonymousRegistrationRequest(
            deviceId: "device-installation-1234", installationSecret: String(repeating: "b", count: 43), platform: "IOS", appVersion: "0.1.0", timezone: "UTC"
        )

        do {
            _ = try await client.register(registration)
            XCTFail("Expected an HTTP error")
        } catch let error as SyncAPIError {
            XCTAssertEqual(error, .httpStatus(400, "INVALID_REQUEST"))
        }
    }

    func testMissingEndpointDoesNotCallTransport() async throws {
        let transport = AnonymousAuthStubTransport(response: SyncHTTPResponse(statusCode: 201, body: Data("{}".utf8)))
        let client = AnonymousAuthAPIClient(configuration: SyncAPIConfiguration(value: nil), transport: transport)
        let registration = try AnonymousRegistrationRequest(
            deviceId: "device-installation-1234", installationSecret: String(repeating: "c", count: 43), platform: "IOS", appVersion: "0.1.0", timezone: "UTC"
        )

        do {
            _ = try await client.register(registration)
            XCTFail("Expected registration to remain disabled")
        } catch let error as SyncAPIError {
            XCTAssertEqual(error, .notConfigured)
        }
        let request = await transport.request()
        XCTAssertNil(request)
    }

    func testBindsInstallationSecretWithExistingBearer() async throws {
        let transport = AnonymousAuthStubTransport(response: SyncHTTPResponse(statusCode: 201, body: Data("{\"bound\":true}".utf8)))
        let client = AnonymousAuthAPIClient(
            configuration: SyncAPIConfiguration(value: "https://api.example.test/api/v1"), transport: transport
        )
        try await client.bindInstallation(String(repeating: "d", count: 43), bearerToken: "existing-token")
        let capturedRequest = await transport.request()
        let request = try XCTUnwrap(capturedRequest)
        XCTAssertEqual(request.url.absoluteString, "https://api.example.test/api/v1/auth/anonymous/credentials")
        XCTAssertEqual(request.bearerToken, "existing-token")
    }
}

private actor AnonymousAuthStubTransport: SyncHTTPTransport {
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
