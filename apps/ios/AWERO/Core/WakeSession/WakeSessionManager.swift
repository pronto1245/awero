import Foundation

@MainActor
final class WakeSessionManager {
    private(set) var current: WakeSession?
    private var transitionInProgress = false
    private var transitionWaiters: [CheckedContinuation<Void, Never>] = []
    private let database: CoreDataStore

    init(database: CoreDataStore = .shared) {
        self.database = database
    }

    func restore() async -> WakeSession? {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        return current
    }

    func trigger(alarm: Alarm, scheduledAt: Date) async -> Bool {
        await acquireTransition()
        defer { releaseTransition() }
        if let existing = await database.fetchActiveWakeSession() {
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
        guard await database.saveWakeSession(session) else { return false }
        current = session
        return true
    }

    func startMission() async {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        guard var session = current, session.result == nil, session.missionStartedAt == nil else { return }
        session.missionStartedAt = .now
        guard await database.saveWakeSession(session) else { return }
        current = session
    }

    func complete() async -> WakeSession? {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        guard var session = current, session.result == nil else { return nil }
        session.completedAt = .now
        session.result = "COMPLETED"
        if let start = session.triggeredAt {
            session.completionTimeSeconds = max(0, Int(Date.now.timeIntervalSince(start)))
        }
        guard await database.saveWakeSession(session) else { return nil }
        current = session
        return session
    }

    func setSnoozeCount(_ count: Int) async {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        guard var session = current, session.result == nil, session.snoozeCount != count else { return }
        session.snoozeCount = count
        guard await database.saveWakeSession(session) else { return }
        current = session
    }

    func markFallback() async {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        guard var session = current, session.result == nil, !session.fallbackUsed else { return }
        session.fallbackUsed = true
        guard await database.saveWakeSession(session) else { return }
        current = session
    }

    func emergencyStop() async -> WakeSession? {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        guard var session = current, session.result == nil else { return nil }
        session.emergencyStop = true
        session.result = "EMERGENCY_STOP"
        session.completedAt = .now
        guard await database.saveWakeSession(session) else { return nil }
        current = session
        return session
    }

    private func acquireTransition() async {
        if !transitionInProgress {
            transitionInProgress = true
            return
        }
        await withCheckedContinuation { continuation in
            transitionWaiters.append(continuation)
        }
    }

    private func releaseTransition() {
        if transitionWaiters.isEmpty {
            transitionInProgress = false
        } else {
            transitionWaiters.removeFirst().resume()
        }
    }

    private func ensureCurrent() async {
        if current == nil {
            current = await database.fetchActiveWakeSession()
        }
    }
}
