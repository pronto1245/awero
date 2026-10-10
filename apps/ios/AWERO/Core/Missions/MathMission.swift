import Foundation

struct MathProblem {
    let left: Int
    let right: Int
    let operation: Character
    var answer: Int { operation == "+" ? left + right : left - right }
}

final class MathMission {
    let difficulty: Difficulty
    private(set) var problem: MathProblem?
    init(difficulty: Difficulty) { self.difficulty = difficulty }
    func start() {
        let upperBound = difficulty == .easy ? 10 : difficulty == .medium ? 25 : 50
        let operation: Character = Bool.random() ? "+" : "-"
        let left = Int.random(in: 1...upperBound)
        let right = Int.random(in: 1...upperBound)
        problem = operation == "-"
            ? MathProblem(left: max(left, right), right: min(left, right), operation: operation)
            : MathProblem(left: left, right: right, operation: operation)
    }
    func validate(answer: Int) -> Bool { problem?.answer == answer }
}
