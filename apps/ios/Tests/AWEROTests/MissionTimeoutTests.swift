import SwiftUI
import XCTest
@testable import AWERO

@MainActor
final class MissionTimeoutTests: XCTestCase {
    func testStepsTimeoutRequestsFallbackWithoutReportingSuccess() async {
        let fallback = expectation(description: "Steps timeout requests local fallback")
        fallback.assertForOverFulfill = true
        let alarm = Alarm(hour: 7, minute: 30, missionType: .steps, difficulty: .easy)
        let view = MissionView(
            alarm: alarm,
            onSuccess: { XCTFail("A timeout must not count as mission success") },
            onFailure: { fallback.fulfill() },
            timeout: .zero
        )
        let window: UIWindow
        if let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first {
            window = UIWindow(windowScene: scene)
        } else {
            window = UIWindow(frame: UIScreen.main.bounds)
        }
        window.rootViewController = UIHostingController(rootView: view)
        window.makeKeyAndVisible()
        defer { window.isHidden = true; window.rootViewController = nil }
        await fulfillment(of: [fallback], timeout: 5)
    }
}
