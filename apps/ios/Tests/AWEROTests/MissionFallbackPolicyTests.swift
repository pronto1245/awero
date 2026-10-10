import XCTest
@testable import AWERO

final class MissionFallbackPolicyTests: XCTestCase {
    func testCameraAndMotionMissionsUseMathFallback() {
        for mission in [MissionType.qr, .steps, .photo, .mixed] {
            XCTAssertEqual(MissionFallbackPolicy.next(after: mission), .math)
        }
    }

    func testMathHasNoFurtherFallback() {
        XCTAssertNil(MissionFallbackPolicy.next(after: .math))
    }
}
