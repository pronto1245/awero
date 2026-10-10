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
    @Published private(set) var loadFailed = false
    private let database: CoreDataStore
    private let defaults: UserDefaults
    private var loaded = false
    private var writeInProgress = false
    private var writeWaiters: [CheckedContinuation<Void, Never>] = []

    init(database: CoreDataStore = .shared, defaults: UserDefaults = .standard) {
        self.database = database
        self.defaults = defaults
        statistics = WakeStatistics()
        Task { await load() }
    }

    func load() async {
        await acquireWrite()
        defer { releaseWrite() }
        await loadStoredStatistics()
    }

    private func loadStoredStatistics() async {
        loaded = false
        loadFailed = false
        switch await database.fetchStatistics() {
        case let .success(stored?):
            statistics = stored
            loaded = true
        case .failure:
            loadFailed = true
        case .success(nil):
            let key = "awero.statistics.v1"
            let migrationKey = "awero.coredata.statistics.migrated.v1"
            guard !defaults.bool(forKey: migrationKey) else {
                loaded = true
                return
            }
            guard let data = defaults.data(forKey: key) else {
                defaults.set(true, forKey: migrationKey)
                loaded = true
                return
            }
            guard let legacy = try? JSONDecoder().decode(WakeStatistics.self, from: data),
                  await database.saveStatistics(legacy) else {
                loadFailed = true
                return
            }
            statistics = legacy
            defaults.set(true, forKey: migrationKey)
            loaded = true
        }
    }

    func recordPlanned() async {
        await acquireWrite()
        defer { releaseWrite() }
        await ensureLoaded()
        guard loaded else { return }
        var next = statistics
        next.planned += 1
        guard await database.saveStatistics(next) else { return }
        statistics = next
    }

    func record(_ session: WakeSession) async {
        guard !session.isTest else { return }
        await acquireWrite()
        defer { releaseWrite() }
        await ensureLoaded()
        guard loaded else { return }
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
