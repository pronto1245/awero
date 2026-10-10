import Foundation

enum AlarmTimeFormatter {
    static func string(
        hour: Int,
        minute: Int,
        timeZone: TimeZone = .current,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        var components = DateComponents()
        components.year = 2024
        components.month = 1
        components.day = 1
        components.hour = hour
        components.minute = minute
        let date = calendar.date(from: components) ?? Date(timeIntervalSince1970: 0)

        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeZone = timeZone
        formatter.dateStyle = .none
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}
