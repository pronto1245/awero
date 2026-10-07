import Foundation
import Combine

@MainActor
final class WakeFlowController: ObservableObject {
    enum State: Equatable { case idle, ringing, mission, completed, emergencyStopped }
    @Published private(set) var state: State = .idle
    @Published private(set) var currentMission: MissionType = .math
    @Published private(set) var snoozeCount = 0
    private let sessionManager: WakeSessionManager
    private var maxSnoozes = 3
    private var snoozeMinutes = 10

    init(sessionManager: WakeSessionManager = WakeSessionManager()) { self.sessionManager = sessionManager }

    func start(alarm: Alarm, scheduledAt: Date = .now) {
        currentMission = alarm.missionType
        maxSnoozes = alarm.maxSnoozes
        snoozeMinutes = alarm.snoozeMinutes
        snoozeCount = 0
        sessionManager.trigger(alarm: alarm, scheduledAt: scheduledAt)
        state = .ringing
    }

    func beginMission() { guard state == .ringing else { return }; state = .mission; sessionManager.startMission() }
    func completeMission() { guard state == .mission else { return }; sessionManager.complete(); state = .completed }

    func snooze() {
        guard state == .ringing, snoozeCount < maxSnoozes else { return }
        snoozeCount += 1
        sessionManager.setSnoozeCount(snoozeCount)
        state = .idle
        _ = snoozeMinutes
    }

    func emergencyStop() { sessionManager.emergencyStop(); state = .emergencyStopped }
}