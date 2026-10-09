import XCTest
@testable import AWERO

final class AlarmTimeFormatterTests: XCTestCase {
    func testUSLocaleUsesLocalizedTwelveHourTime() {
        let time = AlarmTimeFormatter.string(
            hour: 7,
            minute: 0,
            timeZone: TimeZone(secondsFromGMT: 0)!,
            locale: Locale(identifier: "en_US")
        )

        XCTAssertTrue(time.contains("7"))
        XCTAssertTrue(time.localizedCaseInsensitiveContains("AM"))
        XCTAssertNotEqual(time, "07:00")
    }

    func testRussianLocaleUsesLocalizedTwentyFourHourTime() {
        let time = AlarmTimeFormatter.string(
            hour: 7,
            minute: 0,
            timeZone: TimeZone(secondsFromGMT: 0)!,
            locale: Locale(identifier: "ru_RU")
        )

        XCTAssertEqual(time, "07:00")
    }
}
