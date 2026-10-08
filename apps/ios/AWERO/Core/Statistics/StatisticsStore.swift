import Foundation
import Combine

struct WakeStatistics: Codable, Sendable {
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
    private let database: CoreDataStore
    private var loaded = false

    init(database: CoreDataStore = .shared) {
        self.database = database
        statistics = WakeStatistics()
        Task { await load() }
    }

    func load() async {
        if let stored = await database.fetchStatistics() {
            statistics = stored
            loaded = true
            return
        }

        let key = "awero.statistics.v1"
        if let data = UserDefaults.standard.data(forKey: key),
           let legacy = try? JSONDecoder().decode(WakeStatistics.self, from: data) {
            statistics = legacy
            await database.saveStatistics(legacy)
            UserDefaults.standard.set(true, forKey: "awero.coredata.statistics.migrated.v1")
        }
        loaded = true
    }

    func recordPlanned() async {
        await ensureLoaded()
        statistics.planned += 1
        await database.saveStatistics(statistics)
    }

    func record(_ session: WakeSession) async {
        await ensureLoaded()
        statistics.completed += session.result == "COMPLETED" ? 1 : 0
        statistics.snoozes += session.snoozeCount
        statistics.fallback += session.fallbackUsed ? 1 : 0
        statistics.emergencyStops += session.emergencyStop ? 1 : 0
        statistics.totalCompletionSeconds += session.completionTimeSeconds ?? 0
        await database.saveStatistics(statistics)
    }

    private func ensureLoaded() async {
        if !loaded { await load() }
    }
}
