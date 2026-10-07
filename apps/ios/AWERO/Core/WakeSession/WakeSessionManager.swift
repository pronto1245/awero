import Foundation

@MainActor
final class WakeSessionManager {
    private(set) var current: WakeSession?
    private let database = CoreDataStore.shared

    func trigger(alarm: Alarm, scheduledAt: Date) async -> Bool {
        if let existing = await database.fetchActiveWakeSession(),
           existing.alarmId == alarm.id,
           existing.alarmVersion == alarm.version {
            current = existing
            return false
        }

        let session = WakeSession(
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
        current = session
        await database.saveWakeSession(session)
        return true
    }

    func startMission() async {
        current?.missionStartedAt = .now
        if let current { await database.saveWakeSession(current) }
    }

    func complete() async {
        guard var session = current else { return }
        session.completedAt = .now
        session.result = "COMPLETED"
        if let start = session.triggeredAt {
            session.completionTimeSeconds = max(0, Int(Date.now.timeIntervalSince(start)))
        }
        current = session
        await database.saveWakeSession(session)
    }

    func setSnoozeCount(_ count: Int) async {
        current?.snoozeCount = count
        if let current { await database.saveWakeSession(current) }
    }

    func markFallback() async {
        current?.fallbackUsed = true
        if let current { await database.saveWakeSession(current) }
    }

    func emergencyStop() async {
        guard var session = current else { return }
        session.emergencyStop = true
        session.result = "EMERGENCY_STOP"
        session.completedAt = .now
        current = session
        await database.saveWakeSession(session)
    }
}
