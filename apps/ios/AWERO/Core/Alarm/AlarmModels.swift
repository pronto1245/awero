import Foundation

enum AlarmTimezoneMode: String, Codable { case deviceLocal = "DEVICE_LOCAL", fixed = "FIXED" }
enum MissionType: String, Codable { case math = "MATH", qr = "QR", steps = "STEPS", photo = "PHOTO", mixed = "MIXED" }
enum Difficulty: String, Codable { case easy = "EASY", medium = "MEDIUM", hard = "HARD" }

struct Alarm: Identifiable, Codable {
    let id: UUID
    var version: Int
    var hour: Int
    var minute: Int
    var enabled: Bool
    var weekdays: Set<Int>
    var timezoneMode: AlarmTimezoneMode
    var fixedTimezone: String?
    var missionType: MissionType
    var difficulty: Difficulty
    var maxSnoozes: Int
    var snoozeMinutes: Int
    var qrExpectedCode: String?
    var label: String
    var snoozeEnabled: Bool

    init(
        id: UUID = UUID(),
        version: Int = 1,
        hour: Int,
        minute: Int,
        enabled: Bool = true,
        weekdays: Set<Int> = Set(1...7),
        timezoneMode: AlarmTimezoneMode = .deviceLocal,
        fixedTimezone: String? = nil,
        missionType: MissionType = .math,
        difficulty: Difficulty = .medium,
        maxSnoozes: Int = 3,
        snoozeMinutes: Int = 10,
        qrExpectedCode: String? = nil,
        label: String = "Alarm",
        snoozeEnabled: Bool = true
    ) {
        self.id = id
        self.version = version
        self.hour = hour
        self.minute = minute
        self.enabled = enabled
        self.weekdays = weekdays
        self.timezoneMode = timezoneMode
        self.fixedTimezone = fixedTimezone
        self.missionType = missionType
        self.difficulty = difficulty
        self.maxSnoozes = max(0, maxSnoozes)
        self.snoozeMinutes = max(1, snoozeMinutes)
        self.qrExpectedCode = qrExpectedCode
        self.label = label
        self.snoozeEnabled = snoozeEnabled
    }

    private enum CodingKeys: String, CodingKey {
        case id, version, hour, minute, enabled, weekdays, timezoneMode, fixedTimezone
        case missionType, difficulty, maxSnoozes, snoozeMinutes, qrExpectedCode, label, snoozeEnabled
    }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        self.init(
            id: try values.decode(UUID.self, forKey: .id),
            version: try values.decodeIfPresent(Int.self, forKey: .version) ?? 1,
            hour: try values.decode(Int.self, forKey: .hour),
            minute: try values.decode(Int.self, forKey: .minute),
            enabled: try values.decodeIfPresent(Bool.self, forKey: .enabled) ?? true,
            weekdays: try values.decodeIfPresent(Set<Int>.self, forKey: .weekdays) ?? Set(1...7),
            timezoneMode: try values.decodeIfPresent(AlarmTimezoneMode.self, forKey: .timezoneMode) ?? .deviceLocal,
            fixedTimezone: try values.decodeIfPresent(String.self, forKey: .fixedTimezone),
            missionType: try values.decodeIfPresent(MissionType.self, forKey: .missionType) ?? .math,
            difficulty: try values.decodeIfPresent(Difficulty.self, forKey: .difficulty) ?? .medium,
            maxSnoozes: try values.decodeIfPresent(Int.self, forKey: .maxSnoozes) ?? 3,
            snoozeMinutes: try values.decodeIfPresent(Int.self, forKey: .snoozeMinutes) ?? 10,
            qrExpectedCode: try values.decodeIfPresent(String.self, forKey: .qrExpectedCode),
            label: try values.decodeIfPresent(String.self, forKey: .label) ?? "Alarm",
            snoozeEnabled: try values.decodeIfPresent(Bool.self, forKey: .snoozeEnabled) ?? true
        )
    }
}
