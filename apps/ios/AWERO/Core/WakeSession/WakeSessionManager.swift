import Foundation

@MainActor
final class WakeSessionManager {
    private(set) var current: WakeSession?
    private let database: CoreDataStore

    init(database: CoreDataStore = .shared) {
        self.database = database
    }

    func restore() async -> WakeSession? {
        await ensureCurrent()
        return current
    }

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
        await ensureCurrent()
        guard var session = current, session.result == nil else { return }
        session.missionStartedAt = .now
        current = session
        await database.saveWakeSession(session)
    }

    func complete() async -> WakeSession? {
        await ensureCurrent()
        guard var session = current, session.result == nil else { return nil }
        session.completedAt = .now
        session.result = "COMPLETED"
        if let start = session.triggeredAt {
            session.completionTimeSeconds = max(0, Int(Date.now.timeIntervalSince(start)))
        }
        current = session
        await database.saveWakeSession(session)
        return session
    }

    func setSnoozeCount(_ count: Int) async {
        await ensureCurrent()
        guard var session = current, session.result == nil else { return }
        session.snoozeCount = count
        current = session
        await database.saveWakeSession(session)
    }

    func markFallback() async {
        await ensureCurrent()
        guard var session = current, session.result == nil else { return }
        session.fallbackUsed = true
        current = session
        await database.saveWakeSession(session)
    }

    func emergencyStop() async -> WakeSession? {
        await ensureCurrent()
        guard var session = current, session.result == nil else { return nil }
        session.emergencyStop = true
        session.result = "EMERGENCY_STOP"
        session.completedAt = .now
        current = session
        await database.saveWakeSession(session)
        return session
    }

    private func ensureCurrent() async {
        if current == nil {
            current = await database.fetchActiveWakeSession()
        }
    }
}