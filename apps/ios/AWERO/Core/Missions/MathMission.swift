import Foundation

struct MathProblem {
    let left: Int
    let right: Int
    let operation: Character
    var answer: Int { operation == "+" ? left + right : left - right }
}

final class MathMission: Mission {
    let type: MissionType = .math
    let difficulty: Difficulty
    private(set) var problem: MathProblem?

    init(difficulty: Difficulty) { self.difficulty = difficulty }

    func start() {
        let max = difficulty == .easy ? 10 : difficulty == .medium ? 25 : 50
        problem = MathProblem(left: Int.random(in: 1...max), right: Int.random(in: 1...max), operation: Bool.random() ? "+" : "-")
    }

    func validate() -> Bool { false }
    func validate(answer: Int) -> Bool { problem?.answer == answer }
    func retry() { start() }
}