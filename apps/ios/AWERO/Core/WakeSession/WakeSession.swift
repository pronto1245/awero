import Foundation

enum WakeSessionState {
    case scheduled
    case triggered
    case awake
    case mission
    case validated
    case completed
    case missionFailed
    case fallback
}

struct WakeSession: Identifiable, Codable {
    let id: UUID
    let alarmId: UUID
    let alarmVersion: Int
    let scheduledAt: Date
    var triggeredAt: Date?
    var missionStartedAt: Date?
    var completedAt: Date?
    var result: String?
    var missionType: MissionType
    var completionTimeSeconds: Int?
    var snoozeCount: Int
    var fallbackUsed: Bool
    var emergencyStop: Bool
    var isTest: Bool = false
}
