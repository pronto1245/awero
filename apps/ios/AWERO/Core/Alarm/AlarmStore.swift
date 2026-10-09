import Foundation

enum AlarmStoreLoadState: Equatable {
    case loading
    case loaded
    case failed
}

@MainActor
final class AlarmStore: ObservableObject {
    @Published private(set) var alarms: [Alarm] = []
    @Published private(set) var loadState: AlarmStoreLoadState = .loading
    private let database: CoreDataStore
    private let defaults: UserDefaults

    init(database: CoreDataStore = .shared, defaults: UserDefaults = .standard) {
        self.database = database
        self.defaults = defaults
    }

    func load() async {
        loadState = .loading
        guard case let .success(loadedAlarms) = await database.fetchAlarms() else {
            loadState = .failed
            return
        }
        alarms = loadedAlarms
        guard await migrateLegacyIfNeeded() else {
            loadState = .failed
            return
        }
        loadState = .loaded
    }

    @discardableResult
    func save(_ alarm: Alarm) async -> Bool {
        guard await database.saveAlarm(alarm) else { return false }
        if let index = alarms.firstIndex(where: { $0.id == alarm.id }) {
            alarms[index] = alarm
        } else {
            alarms.append(alarm)
        }
        return true
    }

    func update(_ alarm: Alarm) async {
        var next = alarm
        next.version += 1
        await save(next)
    }

    @discardableResult
    func delete(_ alarm: Alarm) async -> Bool {
        guard await database.deleteAlarm(alarm) else { return false }
        alarms.removeAll { $0.id == alarm.id }
        return true
    }

    private func migrateLegacyIfNeeded() async -> Bool {
        let key = "awero.coredata.migrated.v1"
        guard !defaults.bool(forKey: key) else { return true }

        let legacyKey = "awero.alarms.v1"
        guard let data = defaults.data(forKey: legacyKey) else {
            defaults.set(true, forKey: key)
            return true
        }
        guard let legacy = try? JSONDecoder().decode([Alarm].self, from: data) else { return false }

        for alarm in legacy {
            guard await database.saveAlarm(alarm) else { return false }
        }

        guard case let .success(persistedAlarms) = await database.fetchAlarms() else {
            return false
        }
        let persistedIds = Set(persistedAlarms.map(\.id))
        guard legacy.allSatisfy({ persistedIds.contains($0.id) }) else {
            return false
        }

        alarms = persistedAlarms
        defaults.set(true, forKey: key)
        return true
    }
}
