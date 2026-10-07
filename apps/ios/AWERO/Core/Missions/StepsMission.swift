import Foundation
import CoreMotion

@MainActor
final class StepsMissionRuntime: ObservableObject {
    @Published private(set) var steps = 0
    @Published private(set) var unavailable = false
    private let pedometer = CMPedometer()
    private var target = 30

    func start(target: Int) {
        self.target = max(1, target)
        steps = 0
        guard CMPedometer.isStepCountingAvailable() else {
            unavailable = true
            return
        }
        unavailable = false
        pedometer.startUpdates(from: .now) { [weak self] data, _ in
            guard let self, let count = data?.numberOfSteps.intValue else { return }
            Task { @MainActor in self.steps = count }
        }
    }

    func stop() {
        pedometer.stopUpdates()
    }

    var completed: Bool { steps >= target }
}
