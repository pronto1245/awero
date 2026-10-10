import XCTest
import UIKit

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
        app.scrollViews.firstMatch.swipeUp()
        XCTAssertTrue(app.staticTexts["create.mission_math_body"].waitForExistence(timeout: 5))
        capture("CreateMissions")
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

    func testStepsAndQRWakeStatesUseTheSameVisualSystem() {
        let steps = XCUIApplication()
        steps.launchArguments += ["-awero.uiTestWake", "YES", "-awero.uiTestMission", "STEPS"]
        steps.launch()
        XCTAssertTrue(steps.staticTexts["mission.steps_title"].waitForExistence(timeout: 15))
        XCTAssertTrue(steps.buttons["wake.emergencyStop"].exists)
        capture("Steps")
        steps.buttons["wake.emergencyStop"].tap()
        steps.terminate()

        let qr = XCUIApplication()
        qr.launchArguments += ["-awero.uiTestWake", "YES", "-awero.uiTestMission", "QR"]
        qr.launch()
        XCTAssertTrue(qr.staticTexts["mission.qr_unconfigured"].waitForExistence(timeout: 15))
        XCTAssertTrue(qr.buttons["wake.emergencyStop"].exists)
        capture("QR")
        qr.buttons["wake.emergencyStop"].tap()
    }

    func testLargeTextKeepsCreateActionAndMissionChoicesReachable() {
        let app = XCUIApplication()
        app.launchArguments += [
            "-awero.didCompleteOnboarding.v1", "YES",
            "-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"
        ]
        app.launch()
        let create = app.buttons["home.createAlarm"]
        XCTAssertTrue(create.waitForExistence(timeout: 15))
        capture("HomeLargeText")
        create.tap()
        XCTAssertTrue(app.buttons["alarm.save"].waitForExistence(timeout: 10))
        let missionDescription = app.staticTexts["create.mission_math_body"]
        XCTAssertTrue(missionDescription.waitForExistence(timeout: 10))
        app.scrollViews.firstMatch.swipeUp()
        XCTAssertTrue(missionDescription.isHittable)
        capture("CreateLargeText")
        app.buttons["alarm.cancel"].tap()
    }

    func testAllSupportedLocalesKeepNavigationAndCreationAccessible() {
        for language in ["en", "ru", "pt-BR", "fr", "de", "es"] {
            let app = XCUIApplication()
            app.launchArguments += ["-AppleLanguages", "(\(language))", "-AppleLocale", language.replacingOccurrences(of: "-", with: "_"), "-awero.didCompleteOnboarding.v1", "YES"]
            app.launch()
            let create = app.buttons["home.createAlarm"]
            XCTAssertTrue(create.waitForExistence(timeout: 15), language)
            capture("Home-\(language)")
            app.tabBars.buttons.element(boundBy: 1).tap()
            capture("Progress-\(language)")
            app.tabBars.buttons.element(boundBy: 2).tap()
            capture("Profile-\(language)")
            app.tabBars.buttons.element(boundBy: 0).tap()
            create.tap()
            XCTAssertTrue(app.buttons["alarm.save"].waitForExistence(timeout: 10), language)
            capture("Create-\(language)")
            app.scrollViews.firstMatch.swipeUp()
            XCTAssertTrue(app.staticTexts["create.mission_math_body"].waitForExistence(timeout: 5), language)
            capture("CreateMissions-\(language)")
            app.buttons["alarm.cancel"].tap()
            XCTAssertTrue(create.waitForExistence(timeout: 5), language)
            app.buttons["home.settings"].tap()
            XCTAssertTrue(app.navigationBars.firstMatch.waitForExistence(timeout: 5), language)
            app.navigationBars.buttons.element(boundBy: 0).tap()
            XCTAssertTrue(create.waitForExistence(timeout: 5), language)
            app.terminate()
        }
    }

    private func capture(_ name: String) {
        let screenshot = XCUIScreen.main.screenshot()
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("awero-visual")
        try! FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try! screenshot.pngRepresentation.write(to: directory.appendingPathComponent(name + ".png"))
        let image = screenshot.image
        let width: CGFloat = 390
        let size = CGSize(width: width, height: image.size.height * width / image.size.width)
        let renderer = UIGraphicsImageRenderer(size: size, format: {
            let format = UIGraphicsImageRendererFormat()
            format.scale = 1
            return format
        }())
        let preview = renderer.image { _ in image.draw(in: CGRect(origin: .zero, size: size)) }
        let bytes = Array(preview.jpegData(compressionQuality: 0.65)!.base64EncodedString().utf8)
        for start in stride(from: 0, to: bytes.count, by: 3000) {
            let chunk = String(decoding: bytes[start..<min(start + 3000, bytes.count)], as: UTF8.self)
            print("AWERO_VISUAL|ios|\(name)|\(start / 3000)|\(chunk)")
        }
    }
}
