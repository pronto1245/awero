import Foundation

struct AdaptivePolicy {
    func recommendation(stats: WakeStatistics, current: MissionType) -> (MissionType, Difficulty) {
        if stats.snoozes >= max(3, stats.planned / 2) { return (.qr, .hard) }
        if stats.successfulWakeRate < 0.7 { return (.steps, .medium) }
        return (current, .medium)
    }
}