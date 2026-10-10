import Foundation

@MainActor
final class EmergencyStopController {
    private(set) var stopped = false

    func stop() {
        stopped = true
    }

    func reset() {
        stopped = false
    }
}
