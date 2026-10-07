import Foundation
import CoreMotion

final class StepsMission: Mission {
    let type: MissionType = .steps
    let difficulty: Difficulty
    let targetSteps: Int
    private let pedometer = CMPedometer()
    private var startSteps: Int?

    init(difficulty: Difficulty) {
        self.difficulty = difficulty
        switch difficulty {
        case .easy: targetSteps = 15
        case .medium: targetSteps = 30
        case .hard: targetSteps = 60
        }
    }

    func start() {
        startSteps = nil
        guard CMPedometer.isStepCountingAvailable() else { return }
        pedometer.startUpdates(from: .now) { [weak self] data, _ in
            guard let self, let steps = data?.numberOfSteps.intValue else { return }
            if self.startSteps == nil { self.startSteps = 0 }
            self.startSteps = steps
        }
    }

    func validate() -> Bool {
        guard CMPedometer.isStepCountingAvailable() else { return false }
        return (startSteps ?? 0) >= targetSteps
    }

    func retry() {}
    func stop() { pedometer.stopUpdates() }
}
