import Foundation

@MainActor
final class FallbackEngine {
    private(set) var attempts: [MissionType] = []

    func reset() {
        attempts.removeAll()
    }

    func next(after mission: MissionType) -> MissionType? {
        if attempts.isEmpty || attempts.last != mission {
            attempts.append(mission)
        }

        switch mission {
        case .photo:
            return .qr
        case .qr:
            return .math
        case .steps:
            return .math
        case .mixed:
            return .math
        case .math:
            return nil
        }
    }
}
