import Foundation

@MainActor
final class AlarmRecoveryTrigger {
    private let recovery: AlarmRecovery
    init(recovery: AlarmRecovery) { self.recovery = recovery }

    func appDidBecomeActive() {
        Task { await recovery.reconcile() }
    }

    func deviceTimeChanged() {
        Task { await recovery.reconcile() }
    }
}
