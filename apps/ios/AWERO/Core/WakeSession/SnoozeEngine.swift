import Foundation

struct SnoozePolicy {
    let maxSnoozes: Int
    let durationMinutes: Int
}

final class SnoozeEngine {
    private(set) var count = 0
    let policy: SnoozePolicy

    init(policy: SnoozePolicy) {
        self.policy = policy
    }

    var canSnooze: Bool { count < policy.maxSnoozes }

    func snooze() -> Bool {
        guard canSnooze else { return false }
        count += 1
        return true
    }

    func reset() {
        count = 0
    }
}
