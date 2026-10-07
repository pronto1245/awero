import Foundation

struct QRMission: Mission {
    let type: MissionType = .qr
    let difficulty: Difficulty
    let expectedPayload: String

    init(difficulty: Difficulty, expectedPayload: String) {
        self.difficulty = difficulty
        self.expectedPayload = expectedPayload
    }

    func start() {}
    func retry() {}

    func validate() -> Bool {
        false
    }

    func validate(payload: String) -> Bool {
        payload == expectedPayload
    }
}
