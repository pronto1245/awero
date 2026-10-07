import Foundation
import Combine

@MainActor
final class WakeFlowController: ObservableObject {
    enum State: Equatable { case idle, ringing, mission, completed, emergencyStopped }
    @Published private(set) var state: State = .idle
    @Published private(set) var currentMission: MissionType = .math
    @Published private(set) var snoozeCount = 0

    private let sessionManager: WakeSessionManager
    private let scheduler: AlarmScheduler
    private(set) var currentAlarm: Alarm?
    private var maxSnoozes = 3

    init(
        sessionManager: WakeSessionManager = WakeSessionManager(),
        scheduler: AlarmScheduler = AlarmScheduler()
    ) {
        self.sessionManager = sessionManager
        self.scheduler = scheduler
    }

    func start(alarm: Alarm, scheduledAt: Date = .now) {
        currentAlarm = alarm
        currentMission = alarm.missionType
        maxSnoozes = alarm.maxSnoozes
        snoozeCount = 0
        sessionManager.trigger(alarm: alarm, scheduledAt: scheduledAt)
        state = .ringing
    }

    func beginMission() {
        guard state == .ringing else { return }
        state = .mission
        sessionManager.startMission()
    }

    func fallbackToMath() {
        guard state == .mission else { return }
        currentMission = .math
        sessionManager.markFallback()
    }

    func completeMission() {
        guard state == .mission else { return }
        sessionManager.complete()
        state = .completed
    }

    func snooze() {
        guard let alarm = currentAlarm, state == .ringing, snoozeCount < maxSnoozes else { return }
        snoozeCount += 1
        sessionManager.setSnoozeCount(snoozeCount)
        Task {
            try? await scheduler.scheduleSnooze(for: alarm)
        }
        state = .idle
    }

    func emergencyStop() {
        sessionManager.emergencyStop()
        state = .emergencyStopped
    }
}
