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

        let saveButton = app.buttons["alarm.save"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 10))
        saveButton.tap()

        let permissionContinue = app.alerts.buttons["permission.continue"]
        XCTAssertTrue(permissionContinue.waitForExistence(timeout: 10))
        permissionContinue.tap()

        XCTAssertTrue(app.staticTexts["home.alarm.time"].waitForExistence(timeout: 15))
    }
}
