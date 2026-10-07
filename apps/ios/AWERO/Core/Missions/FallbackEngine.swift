import Foundation

struct FallbackStep {
    let mission: MissionType
    let reason: String
}

final class FallbackEngine {
    private let chain: [MissionType]

    init(primary: MissionType) {
        switch primary {
        case .photo:
            chain = [.photo, .qr, .math]
        case .steps:
            chain = [.steps, .qr, .math]
        case .qr:
            chain = [.qr, .math]
        case .mixed:
            chain = [.mixed, .qr, .math]
        case .math:
            chain = [.math]
        }
    }

    func next(after mission: MissionType) -> MissionType? {
        guard let index = chain.firstIndex(of: mission) else { return chain.first }
        let nextIndex = index + 1
        return nextIndex < chain.count ? chain[nextIndex] : nil
    }

    func canFallback(after mission: MissionType) -> Bool {
        next(after: mission) != nil
    }
}
