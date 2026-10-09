import Foundation

struct QRMission {
    let expectedPayload: String

    init(expectedPayload: String) {
        self.expectedPayload = expectedPayload
    }

    func validate(payload: String) -> Bool {
        payload == expectedPayload
    }
}
