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

    func trigger(alarm: Alarm, scheduledAt: Date, isTest: Bool = false) async -> Bool {
        await acquireTransition()
        defer { releaseTransition() }
        if let existing = await database.fetchActiveWakeSession(alarmID: alarm.id, isTest: isTest) {
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
            emergencyStop: false,
            isTest: isTest
        )
        guard await database.saveWakeSession(session) else { return false }
        current = session
        return true
    }

    @discardableResult
    func startMission() async -> Bool {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        guard var session = current, session.result == nil, session.missionStartedAt == nil else { return false }
        session.missionStartedAt = .now
        guard await database.saveWakeSession(session) else { return false }
        current = session
        return true
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

    @discardableResult
    func setSnoozeCount(_ count: Int) async -> Bool {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        guard var session = current, session.result == nil, session.snoozeCount != count else { return false }
        session.snoozeCount = count
        guard await database.saveWakeSession(session) else { return false }
        current = session
        return true
    }

    @discardableResult
    func markFallback() async -> Bool {
        await acquireTransition()
        defer { releaseTransition() }
        await ensureCurrent()
        guard var session = current, session.result == nil, !session.fallbackUsed else { return false }
        session.fallbackUsed = true
        guard await database.saveWakeSession(session) else { return false }
        current = session
        return true
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
