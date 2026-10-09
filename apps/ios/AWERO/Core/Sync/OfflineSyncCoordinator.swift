import Foundation

actor OfflineSyncCoordinator {
    static let shared = OfflineSyncCoordinator()
    private let configuration: SyncAPIConfiguration
    private let sessionStore: AnonymousAuthSessionStore
    private var isRunning = false

    init(configuration: SyncAPIConfiguration = SyncAPIConfiguration(), sessionStore: AnonymousAuthSessionStore = AnonymousAuthSessionStore()) {
        self.configuration = configuration
        self.sessionStore = sessionStore
    }

    func runOnce(now: Date = .now) async {
        guard !isRunning else { return }
        isRunning = true
        defer { isRunning = false }
        guard configuration.isEnabled else { return }
        await AnonymousAuthCoordinator(configuration: configuration, store: sessionStore).refreshIfNeeded(now: now)

        do {
            guard let session = try sessionStore.loadSession(), session.expiresAt > now else { return }
            let queue = SyncQueueStore.shared
            let operations = await queue.due()
            guard !operations.isEmpty else { return }

            let response: SyncBatchResponse
            do {
                response = try await SyncAPIClient(configuration: configuration)
                    .sync(bearerToken: session.accessToken, operations: operations)
            } catch SyncAPIError.httpStatus(401, _) {
                try sessionStore.clearSession()
                await AnonymousAuthCoordinator(configuration: configuration, store: sessionStore).refreshIfNeeded(now: now)
                guard let refreshed = try sessionStore.loadSession(), refreshed.expiresAt > now else { throw SyncAPIError.httpStatus(401, nil) }
                response = try await SyncAPIClient(configuration: configuration)
                    .sync(bearerToken: refreshed.accessToken, operations: operations)
            }

            let accepted = Set(response.acceptedIds)
            let conflictsById = Dictionary(uniqueKeysWithValues: response.conflicts.map { ($0.id, $0) })
            for operation in operations {
                if accepted.contains(operation.id) {
                    await queue.acknowledge(operation.id)
                } else if let conflict = conflictsById[operation.id] {
                    if !(await queue.recordConflict(operation, conflict: conflict)) {
                        await queue.retry(operation.id)
                    }
                } else {
                    await queue.retry(operation.id)
                }
            }
        } catch {
            let operations = await SyncQueueStore.shared.due()
            for operation in operations {
                await SyncQueueStore.shared.retry(operation.id)
            }
        }
    }
}
