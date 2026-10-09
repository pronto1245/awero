import XCTest
@testable import AWERO

final class SyncAPIConfigurationTests: XCTestCase {
    func testMissingOrInsecureAddressDisablesSync() {
        XCTAssertFalse(SyncAPIConfiguration(value: nil).isEnabled)
        XCTAssertFalse(SyncAPIConfiguration(value: " ").isEnabled)
        XCTAssertFalse(SyncAPIConfiguration(value: "http://api.example.test/api/v1").isEnabled)
        XCTAssertFalse(SyncAPIConfiguration(value: "https://user:pass@api.example.test").isEnabled)
    }

    func testHTTPSAddressBuildsEndpointAndRejectsTraversal() throws {
        let configuration = SyncAPIConfiguration(value: " https://api.example.test/api/v1/ ")

        XCTAssertTrue(configuration.isEnabled)
        XCTAssertEqual(try XCTUnwrap(configuration.endpoint("/health")).absoluteString,
                       "https://api.example.test/api/v1/health")
        XCTAssertNil(configuration.endpoint("../private"))
        XCTAssertNil(configuration.endpoint("%2e%2e/private"))
        XCTAssertNil(configuration.endpoint("health?override=true"))
    }
}
