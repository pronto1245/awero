import Foundation

@MainActor
final class WakeSessionStore {
    private let key = "awero.wake_sessions.v1"

    func load() -> [WakeSession] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let sessions = try? JSONDecoder().decode([WakeSession].self, from: data) else {
            return []
        }
        return sessions
    }

    func save(_ session: WakeSession) {
        var sessions = load()
        if let index = sessions.firstIndex(where: { $0.id == session.id }) {
            sessions[index] = session
        } else {
            sessions.append(session)
        }
        sessions.sort { $0.scheduledAt > $1.scheduledAt }
        sessions = Array(sessions.prefix(200))
        if let data = try? JSONEncoder().encode(sessions) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    func recent(limit: Int = 30) -> [WakeSession] {
        Array(load().prefix(limit))
    }
}
