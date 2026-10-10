import XCTest

final class FirstRunOnboardingUITests: XCTestCase {
    func testFirstRunOnboardingOpensAlarmCreation() {
        let app = XCUIApplication()
        app.launchArguments += ["-awero.didCompleteOnboarding.v1", "NO"]
        app.launch()

        let title = app.staticTexts["onboarding.title"]
        XCTAssertTrue(title.waitForExistence(timeout: 10))

        let continueButton = app.buttons["onboarding.createAlarm"]
        XCTAssertTrue(continueButton.exists)
        XCTAssertFalse(continueButton.label.isEmpty)
        continueButton.tap()

        XCTAssertTrue(app.buttons["alarm.save"].waitForExistence(timeout: 10))
    }
}
