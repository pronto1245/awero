import Foundation

@MainActor
final class AlarmRecoveryTrigger {
    private let coordinator: AlarmCoordinator
    private var isReconciling = false
    private var forceReschedulePending = false

    init(store: AlarmStore) {
        coordinator = AlarmCoordinator(store: store)
    }

    func appDidBecomeActive() {
        enqueue(forceReschedule: false)
    }

    func deviceTimeChanged() {
        enqueue(forceReschedule: true)
    }

    private func enqueue(forceReschedule: Bool) {
        forceReschedulePending = forceReschedulePending || forceReschedule
        guard !isReconciling else { return }

        isReconciling = true
        Task { @MainActor in
            repeat {
                let shouldForceReschedule = forceReschedulePending
                forceReschedulePending = false
                await coordinator.repairAll(forceReschedule: shouldForceReschedule)
            } while forceReschedulePending

            isReconciling = false
        }
    }
}
