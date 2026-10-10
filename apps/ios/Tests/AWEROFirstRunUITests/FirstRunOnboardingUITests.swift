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

        let permissionContinue = app.alerts.buttons["Continue"].firstMatch
        XCTAssertTrue(permissionContinue.waitForExistence(timeout: 10))
        permissionContinue.tap()

        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        for _ in 0..<2 {
            let systemAlert = springboard.alerts.firstMatch
            guard systemAlert.waitForExistence(timeout: 3) else { break }
            let allowButton = systemAlert.buttons.matching(
                NSPredicate(format: "label ==[c] %@ OR label ==[c] %@", "Allow", "Always Allow")
            ).firstMatch
            guard allowButton.exists else { break }
            allowButton.tap()
        }

        let homeAlarm = app.staticTexts["home.alarm.time"]
        if !homeAlarm.waitForExistence(timeout: 15) {
            XCTFail("The first alarm did not appear after saving. Current UI: \(app.debugDescription)")
        }
    }
}
