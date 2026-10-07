import Foundation

struct MathMission: Mission {
    let type: MissionType = .math
    let difficulty: Difficulty
    let left: Int
    let right: Int

    init(difficulty: Difficulty) {
        self.difficulty = difficulty
        switch difficulty {
        case .easy:
            left = 7; right = 5
        case .medium:
            left = 17; right = 8
        case .hard:
            left = 27; right = 16
        }
    }

    func start() {}
    func retry() {}

    func validate() -> Bool {
        false
    }

    var answer: Int { left + right }
}
