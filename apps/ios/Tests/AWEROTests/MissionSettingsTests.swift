import XCTest
@testable import AWERO

@MainActor
final class MissionSettingsTests: XCTestCase {
    func testSavedAndEditedCodeIsUsedForLocalValidationAfterRestart() async throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-mission-settings-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let url = directory.appendingPathComponent("AWERO.sqlite")
        let store = CoreDataStore(storeURL: url)
        let originalCode = "awero://wake/bedroom?code=one+two"
        var alarm = Alarm(hour: 7, minute: 30, missionType: .qr, difficulty: .hard,
                          qrExpectedCode: originalCode, label: "Bedroom", snoozeEnabled: false)
        let saved = await store.saveAlarm(alarm)
        XCTAssertTrue(saved)
        let restarted = CoreDataStore(storeURL: url)
        let restored = try await restarted.fetchAlarm(id: alarm.id).get()
        XCTAssertEqual(restored?.difficulty, .hard)
        XCTAssertEqual(restored?.qrExpectedCode, originalCode)
        XCTAssertEqual(restored?.label, "Bedroom")
        XCTAssertEqual(restored?.snoozeEnabled, false)

        alarm.qrExpectedCode = "0123456789012"
        alarm.version += 1
        let edited = await restarted.saveAlarm(alarm)
        XCTAssertTrue(edited)
        let reopened = CoreDataStore(storeURL: url)
        let latest = try await reopened.fetchAlarm(id: alarm.id).get()
        let latestCode = try XCTUnwrap(latest?.qrExpectedCode)
        let mission = QRMission(expectedPayload: latestCode)
        XCTAssertTrue(mission.validate(payload: "0123456789012"))
        XCTAssertFalse(mission.validate(payload: originalCode))
        XCTAssertEqual(latest?.version, 2)
    }
}
