import Foundation

enum MissionState { case ready, started, inProgress, success, retry, timeout, failure, fallback }

protocol Mission {
    var type: MissionType { get }
    var difficulty: Difficulty { get }
    func start()
    func validate() -> Bool
    func retry()
}

@MainActor
final class MissionEngine {
    private(set) var state: MissionState = .ready
    func start(_ mission: Mission) { state = .started; mission.start(); state = .inProgress }
    func validate(_ mission: Mission) -> Bool { let success = mission.validate(); state = success ? .success : .retry; return success }
    func fallback() { state = .fallback }
}