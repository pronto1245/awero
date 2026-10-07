import Foundation

enum MissionState {
    case ready
    case started
    case inProgress
    case success
    case retry
    case timeout
    case failure
    case fallback
}

protocol Mission {
    var type: MissionType { get }
    var difficulty: Difficulty { get }
    func start()
    func validate() -> Bool
    func retry()
}

final class MissionEngine {
    private(set) var state: MissionState = .ready

    func start<M: Mission>(_ mission: M) {
        state = .started
        mission.start()
        state = .inProgress
    }

    func validate<M: Mission>(_ mission: M) -> Bool {
        let success = mission.validate()
        state = success ? .success : .retry
        return success
    }

    func fallback() {
        state = .fallback
    }
}
