import Foundation

struct WakeStatistics: Codable {
    var planned = 0
    var completed = 0
    var failed = 0
    var emergencyStops = 0
    var totalSnoozes = 0
    var totalCompletionSeconds = 0
    var fallbackCount = 0

    var successfulWakeRate: Double {
        planned == 0 ? 0 : Double(completed) / Double(planned)
    }

    var averageCompletionSeconds: Double {
        completed == 0 ? 0 : Double(totalCompletionSeconds) / Double(completed)
    }
}

@MainActor
final class StatisticsStore {
    private let key = "awero.statistics.v1"

    func load() -> WakeStatistics {
        guard let data = UserDefaults.standard.data(forKey: key),
              let value = try? JSONDecoder().decode(WakeStatistics.self, from: data) else {
            return WakeStatistics()
        }
        return value
    }

    func record(_ session: WakeSession) {
        var stats = load()
        stats.planned += 1
        stats.totalSnoozes += session.snoozeCount
        if session.fallbackUsed { stats.fallbackCount += 1 }
        if session.emergencyStop {
            stats.emergencyStops += 1
            stats.failed += 1
        } else if session.result == "COMPLETED" {
            stats.completed += 1
            stats.totalCompletionSeconds += session.completionTimeSeconds ?? 0
        } else {
            stats.failed += 1
        }
        if let data = try? JSONEncoder().encode(stats) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
}
