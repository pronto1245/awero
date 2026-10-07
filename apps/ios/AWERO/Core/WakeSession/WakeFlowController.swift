import Foundation

@MainActor
final class WakeFlowController {
    let sessionManager = WakeSessionManager()
    let sessionStore = WakeSessionStore()
    let statisticsStore = StatisticsStore()

    private(set) var fallbackEngine: FallbackEngine?
    private(set) var snoozeEngine: SnoozeEngine?

    func begin(alarm: Alarm, scheduledAt: Date) {
        sessionManager.trigger(alarm: alarm, scheduledAt: scheduledAt)
        fallbackEngine = FallbackEngine(primary: alarm.missionType)
        snoozeEngine = SnoozeEngine(policy: SnoozePolicy(
            maxSnoozes: alarm.maxSnoozes,
            durationMinutes: alarm.snoozeMinutes
        ))
        persist()
    }

    func startMission() {
        sessionManager.startMission()
        persist()
    }

    func snooze() -> Bool {
        guard let engine = snoozeEngine, engine.snooze() else { return false }
        sessionManager.setSnoozeCount(engine.count)
        persist()
        return true
    }

    func fallback(from mission: MissionType) -> MissionType? {
        guard let next = fallbackEngine?.next(after: mission) else { return nil }
        sessionManager.markFallback()
        persist()
        return next
    }

    func complete() {
        sessionManager.complete()
        persistAndRecord()
    }

    func emergencyStop() {
        sessionManager.emergencyStop()
        persistAndRecord()
    }

    private func persist() {
        if let session = sessionManager.current { sessionStore.save(session) }
    }

    private func persistAndRecord() {
        guard let session = sessionManager.current else { return }
        sessionStore.save(session)
        statisticsStore.record(session)
    }
}
