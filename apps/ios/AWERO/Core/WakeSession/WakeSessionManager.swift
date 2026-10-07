import Foundation

@MainActor
final class WakeSessionManager {
    private(set) var current: WakeSession?

    func trigger(alarm: Alarm, scheduledAt: Date) {
        current = WakeSession(
            id: UUID(),
            alarmId: alarm.id,
            alarmVersion: alarm.version,
            scheduledAt: scheduledAt,
            triggeredAt: .now,
            missionStartedAt: nil,
            completedAt: nil,
            result: nil,
            missionType: alarm.missionType,
            completionTimeSeconds: nil,
            snoozeCount: 0,
            fallbackUsed: false,
            emergencyStop: false
        )
    }

    func startMission() {
        current?.missionStartedAt = .now
    }

    func complete() {
        guard var session = current else { return }
        session.completedAt = .now
        session.result = "COMPLETED"
        if let start = session.triggeredAt {
            session.completionTimeSeconds = max(0, Int(Date.now.timeIntervalSince(start)))
        }
        current = session
    }

    func emergencyStop() {
        current?.emergencyStop = true
        current?.result = "EMERGENCY_STOP"
        current?.completedAt = .now
    }
}
