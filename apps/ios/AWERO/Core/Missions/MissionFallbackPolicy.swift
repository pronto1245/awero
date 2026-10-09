import Foundation

enum MissionFallbackPolicy {
    static func next(after mission: MissionType) -> MissionType? {
        switch mission {
        case .math: return nil
        case .qr, .steps, .photo, .mixed: return .math
        }
    }
}
