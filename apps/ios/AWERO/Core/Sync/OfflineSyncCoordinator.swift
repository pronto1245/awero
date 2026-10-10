import Foundation
import os

actor OfflineSyncCoordinator {
    static let shared = OfflineSyncCoordinator()
    private let configuration: SyncAPIConfiguration
    private let sessionStore: AnonymousAuthSessionStore
    private let logger = Logger(subsystem: "app.awero", category: "sync")
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
            let analyticsQueue = AnalyticsQueueStore.shared
            if case let .success(events) = await analyticsQueue.pending(), !events.isEmpty {
                let analyticsResponse = try await SyncAPIClient(configuration: configuration)
                    .sendAnalytics(bearerToken: session.accessToken, events: events)
                let completed = Set(analyticsResponse.acceptedIds).union(analyticsResponse.rejected.map(\.id))
                for event in events where completed.contains(event.id) {
                    await analyticsQueue.acknowledge(event.id)
                }
            }
            let queue = SyncQueueStore.shared
            guard case let .success(operations) = await queue.due() else {
                logger.error("Could not read the persisted sync queue; queued operations were left untouched.")
                return
            }
            guard !operations.isEmpty else {
                let conflicts = await queue.conflicts()
                let protected = (try? conflicts.get()).map { Set($0.map { UUID(uuidString: $0.entityId) }.compactMap { $0 }) } ?? []
                await AlarmSnapshotSyncClient(configuration: configuration).pullAndApply(
                    bearerToken: session.accessToken, protectedAlarmIDs: protected
                )
                await WakeSessionSyncClient(configuration: configuration).uploadRecent(bearerToken: session.accessToken, now: now)
                return
            }

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
            let rejectionsById = Dictionary(uniqueKeysWithValues: response.rejected.map { ($0.id, $0) })
            for operation in operations {
                if accepted.contains(operation.id) {
                    await queue.acknowledge(operation.id)
                } else if let conflict = conflictsById[operation.id] {
                    if !(await queue.recordConflict(operation, conflict: conflict)) {
                      await queue.retry(operation.id)
                    }
                } else if let rejection = rejectionsById[operation.id] {
                    let terminal = SyncConflict(
                        id: rejection.id, code: rejection.code, serverVersion: nil, serverEntity: nil
                    )
                    if await queue.recordConflict(operation, conflict: terminal) {
                        await queue.acknowledge(operation.id)
                    } else {
                        await queue.retry(operation.id)
                    }
                } else {
                    await queue.retry(operation.id)
                }
            }
            let activeToken = try sessionStore.loadSession()?.accessToken ?? session.accessToken
            let pending = await queue.due()
            let conflicts = await queue.conflicts()
            let protected = Set((try? pending.get()).map { $0.compactMap { UUID(uuidString: $0.entityId) } } ?? [])
                .union((try? conflicts.get()).map { $0.compactMap { UUID(uuidString: $0.entityId) } } ?? [])
            await AlarmSnapshotSyncClient(configuration: configuration).pullAndApply(
                bearerToken: activeToken, protectedAlarmIDs: protected
            )
            await WakeSessionSyncClient(configuration: configuration).uploadRecent(bearerToken: activeToken, now: now)
        } catch {
            logger.error("API sync failed; local alarm and analytics queues remain available for a later retry.")
            if case let SyncAPIError.httpStatus(status, _) = error, status >= 500,
               let session = try? sessionStore.loadSession(),
               let installationID = try? sessionStore.installationID() {
                await SupportDiagnosticsAPIClient(configuration: configuration)
                    .reportSyncServerFailure(statusCode: status, bearerToken: session.accessToken,
                                             installationID: installationID, now: now)
            }
            guard case let .success(operations) = await SyncQueueStore.shared.due() else {
                logger.error("Could not read the persisted sync queue after a sync failure; queued operations were left untouched.")
                return
            }
            for operation in operations {
                await SyncQueueStore.shared.retry(operation.id)
            }
        }
    }
}
