import Foundation
import Combine

@MainActor
final class WakeFlowController: ObservableObject {
    enum State: Equatable {
        case idle
        case ringing
        case mission
        case completed
        case emergencyStopped
    }

    @Published private(set) var state: State = .idle
    @Published private(set) var currentMission: MissionType = .math
    @Published private(set) var snoozeCount = 0

    private let sessionManager: WakeSessionManager
    private let maxSnoozes: Int
    private let snoozeMinutes: Int

    init(sessionManager: WakeSessionManager = WakeSessionManager(), maxSnoozes: Int = 3, snoozeMinutes: Int = 10) {
        self.sessionManager = sessionManager
        self.maxSnoozes = maxSnoozes
        self.snoozeMinutes = snoozeMinutes
    }

    func start(alarm: Alarm) {
        currentMission = alarm.missionType
        snoozeCount = 0
        sessionManager.start(alarm: alarm)
        state = .ringing
    }

    func beginMission() {
        guard state == .ringing else { return }
        state = .mission
        sessionManager.startMission()
    }

    func completeMission() {
        guard state == .mission else { return }
        sessionManager.completeMission()
        state = .completed
    }

    func snooze() {
        guard state == .ringing, snoozeCount < maxSnoozes else { return }
        snoozeCount += 1
        sessionManager.setSnoozeCount(snoozeCount)
        sessionManager.snooze(minutes: snoozeMinutes)
        state = .idle
    }

    func emergencyStop() {
        sessionManager.emergencyStop()
        state = .emergencyStopped
    }
}
