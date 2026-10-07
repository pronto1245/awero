import Foundation

struct MathProblem {
    let left: Int
    let right: Int
    let operation: Character
    var answer: Int {
        operation == "+" ? left + right : left - right
    }
}

struct MathMission: Mission {
    let type: MissionType = .math
    let difficulty: Difficulty
    private(set) var problem: MathProblem?

    init(difficulty: Difficulty) {
        self.difficulty = difficulty
    }

    mutating func start() {
        let max = difficulty == .easy ? 10 : difficulty == .medium ? 25 : 50
        let a = Int.random(in: 1...max)
        let b = Int.random(in: 1...max)
        problem = MathProblem(left: a, right: b, operation: Bool.random() ? "+" : "-")
    }

    func validate() -> Bool { false }
    mutating func retry() { start() }

    func validate(answer: Int) -> Bool {
        guard let problem else { return false }
        return answer == problem.answer
    }
}
