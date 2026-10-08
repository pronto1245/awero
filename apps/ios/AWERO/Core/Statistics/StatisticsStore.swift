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
    private var writeInProgress = false
    private var writeWaiters: [CheckedContinuation<Void, Never>] = []

    init(database: CoreDataStore = .shared) {
        self.database = database
        statistics = WakeStatistics()
        Task { await load() }
    }

    func load() async {
        await acquireWrite()
        defer { releaseWrite() }
        await loadStoredStatistics()
    }

    private func loadStoredStatistics() async {
        if let stored = await database.fetchStatistics() {
            statistics = stored
            loaded = true
            return
        }

        let key = "awero.statistics.v1"
        if let data = UserDefaults.standard.data(forKey: key),
           let legacy = try? JSONDecoder().decode(WakeStatistics.self, from: data) {
            guard await database.saveStatistics(legacy) else { return }
            statistics = legacy
            UserDefaults.standard.set(true, forKey: "awero.coredata.statistics.migrated.v1")
        }
        loaded = true
    }

    func recordPlanned() async {
        await acquireWrite()
        defer { releaseWrite() }
        await ensureLoaded()
        var next = statistics
        next.planned += 1
        guard await database.saveStatistics(next) else { return }
        statistics = next
    }

    func record(_ session: WakeSession) async {
        await acquireWrite()
        defer { releaseWrite() }
        await ensureLoaded()
        var next = statistics
        next.completed += session.result == "COMPLETED" ? 1 : 0
        next.snoozes += session.snoozeCount
        next.fallback += session.fallbackUsed ? 1 : 0
        next.emergencyStops += session.emergencyStop ? 1 : 0
        next.totalCompletionSeconds += session.completionTimeSeconds ?? 0
        guard await database.saveStatistics(next) else { return }
        statistics = next
    }

    private func acquireWrite() async {
        if !writeInProgress {
            writeInProgress = true
            return
        }
        await withCheckedContinuation { continuation in
            writeWaiters.append(continuation)
        }
    }

    private func releaseWrite() {
        if writeWaiters.isEmpty {
            writeInProgress = false
        } else {
            writeWaiters.removeFirst().resume()
        }
    }

    private func ensureLoaded() async {
        if !loaded { await loadStoredStatistics() }
    }
}
