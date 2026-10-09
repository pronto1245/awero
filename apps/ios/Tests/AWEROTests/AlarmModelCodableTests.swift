import XCTest
@testable import AWERO

final class AlarmModelCodableTests: XCTestCase {
    func testAlarmEncodedBeforeLabelWasAddedStillDecodes() throws {
        let legacyAlarm = """
        {"id":"A40C2123-45B6-4789-9ABC-DEF012345678","version":1,"hour":7,"minute":30,"enabled":true,"weekdays":[2,3,4,5,6],"timezoneMode":"DEVICE_LOCAL","fixedTimezone":null,"missionType":"MATH","difficulty":"MEDIUM","maxSnoozes":3,"snoozeMinutes":10,"qrExpectedCode":null}
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(Alarm.self, from: legacyAlarm)

        XCTAssertNil(decoded.label)
        XCTAssertEqual(decoded.syncLabel, "Alarm")
    }
}
