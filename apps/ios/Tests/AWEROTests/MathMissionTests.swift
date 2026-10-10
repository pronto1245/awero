import XCTest
@testable import AWERO

final class MathMissionTests: XCTestCase {
    func testOnlyTheComputedAnswerIsAccepted() throws {
        let mission = MathMission(difficulty: .easy)
        mission.start()
        let problem = try XCTUnwrap(mission.problem)

        XCTAssertTrue(mission.validate(answer: problem.answer))
        XCTAssertFalse(mission.validate(answer: problem.answer + 1))
    }

    func testDifficultyBoundsOperands() throws {
        for (difficulty, bound) in [(Difficulty.easy, 10), (.medium, 25), (.hard, 50)] {
            let mission = MathMission(difficulty: difficulty)
            for _ in 0..<50 {
                mission.start()
                let problem = try XCTUnwrap(mission.problem)
                XCTAssertTrue((1...bound).contains(problem.left))
                XCTAssertTrue((1...bound).contains(problem.right))
            }
        }
    }

    func testSubtractionAnswersFitTheDigitOnlyKeypad() throws {
        let mission = MathMission(difficulty: .hard)
        for _ in 0..<100 {
            mission.start()
            let problem = try XCTUnwrap(mission.problem)
            if problem.operation == "-" { XCTAssertGreaterThanOrEqual(problem.answer, 0) }
        }
    }
}
