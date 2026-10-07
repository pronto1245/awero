import Foundation

enum AlarmTimezoneMode: String, Codable {
    case deviceLocal = "DEVICE_LOCAL"
    case fixed = "FIXED"
}

enum MissionType: String, Codable {
    case math = "MATH"
    case qr = "QR"
    case steps = "STEPS"
    case photo = "PHOTO"
    case mixed = "MIXED"
}

enum Difficulty: String, Codable {
    case easy = "EASY"
    case medium = "MEDIUM"
    case hard = "HARD"
}

struct Alarm: Identifiable, Codable {
    let id: UUID
    var version: Int
    var hour: Int
    var minute: Int
    var enabled: Bool
    var timezoneMode: AlarmTimezoneMode
    var fixedTimezone: String?
    var missionType: MissionType
    var difficulty: Difficulty
    var maxSnoozes: Int
    var snoozeMinutes: Int
}
