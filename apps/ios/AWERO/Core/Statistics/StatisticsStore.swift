import Foundation
import Combine

struct WakeStatistics: Codable {
    var planned = 0
    var completed = 0
    var snoozes = 0
    var fallback = 0
    var emergencyStops = 0
    var totalCompletionSeconds = 0

    var successfulWakeRate: Double { planned == 0 ? 0 : Double(completed) / Double(planned) }
    var averageCompletionSeconds: Double { completed == 0 ? 0 : Double(totalCompletionSeconds) / Double(completed) }
}

@MainActor
final class StatisticsStore: ObservableObject {
    @Published private(set) var statistics: WakeStatistics
    private let key = "awero.statistics.v1"

    init() {
        if let data = UserDefaults.standard.data(forKey: key),
           let value = try? JSONDecoder().decode(WakeStatistics.self, from: data) {
            statistics = value
        } else { statistics = WakeStatistics() }
    }

    func recordPlanned() { statistics.planned += 1; persist() }
    func record(_ session: WakeSession) {
        statistics.completed += session.result == "COMPLETED" ? 1 : 0
        statistics.snoozes += session.snoozeCount
        statistics.fallback += session.fallbackUsed ? 1 : 0
        statistics.emergencyStops += session.emergencyStop ? 1 : 0
        statistics.totalCompletionSeconds += session.completionTimeSeconds ?? 0
        persist()
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(statistics) { UserDefaults.standard.set(data, forKey: key) }
    }
}