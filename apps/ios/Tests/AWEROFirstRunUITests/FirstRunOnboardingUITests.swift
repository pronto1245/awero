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
        capture("Create")
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
        capture("Home")
        app.tabBars.buttons.element(boundBy: 1).tap()
        capture("Progress")
        app.tabBars.buttons.element(boundBy: 2).tap()
        capture("Profile")
    }

    func testMathKeypadCompletesWakeSession() {
        let app = XCUIApplication()
        app.launchArguments += ["-awero.uiTestWake", "YES"]
        app.launch()
        let problem = app.staticTexts["mission.math.problem"]
        XCTAssertTrue(problem.waitForExistence(timeout: 15))
        let parts = problem.label.split(separator: " ")
        guard parts.count >= 3, let left = Int(parts[0]), let right = Int(parts[2]) else {
            XCTFail("Math problem was not readable")
            return
        }
        let answer = parts[1] == "+" ? left + right : left - right
        if answer < 0 { app.buttons["mission.math.sign"].tap() }
        for digit in String(abs(answer)) { app.buttons[String(digit)].tap() }
        app.buttons["mission.math.check"].tap()
        XCTAssertTrue(app.staticTexts["wake.completed"].waitForExistence(timeout: 5))
        capture("MathCompleted")
    }

    func testMathKeypadUsesRealWakeFlow() {
        let app = XCUIApplication()
        app.launchArguments += ["-awero.uiTestWake", "YES"]
        app.launch()
        let problem = app.staticTexts["mission.math.problem"]
        XCTAssertTrue(problem.waitForExistence(timeout: 15))
        capture("Math")
        let parts = problem.label.split(separator: " ")
        guard parts.count >= 3, let left = Int(parts[0]), let right = Int(parts[2]) else {
            XCTFail("Math problem was not readable")
            return
        }
        let answer = parts[1] == "+" ? left + right : left - right
        let value = answer + 1
        if value < 0 { app.buttons["mission.math.sign"].tap() }
        for digit in String(abs(value)) { app.buttons[String(digit)].tap() }
        app.buttons["mission.math.check"].tap()
        XCTAssertTrue(app.staticTexts["mission.math.invalid"].waitForExistence(timeout: 5))
        capture("MathInvalid")
        XCTAssertTrue(app.buttons["wake.emergencyStop"].exists)
        app.buttons["wake.emergencyStop"].tap()
        XCTAssertTrue(app.staticTexts["wake.stopped"].waitForExistence(timeout: 5))
    }

    private func capture(_ name: String) {
        let bytes = Array(XCUIScreen.main.screenshot().pngRepresentation.base64EncodedString().utf8)
        for start in stride(from: 0, to: bytes.count, by: 3000) {
            let chunk = String(decoding: bytes[start..<min(start + 3000, bytes.count)], as: UTF8.self)
            print("AWERO_VISUAL|ios|\(name)|\(start / 3000)|\(chunk)")
        }
    }
}
