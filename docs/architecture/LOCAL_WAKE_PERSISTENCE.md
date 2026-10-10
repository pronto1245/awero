# Local Wake Persistence

Critical wake data is persisted locally before network synchronization.

iOS:
- WakeSessionStore uses local device storage.
- StatisticsStore derives SWR and completion metrics locally.

Android:
- WakeSessionStore persists recent sessions locally.
- WakeFlowController owns the active session state.

Rules:
- UI updates immediately from local state.
- Network is never required to complete a wake session.
- Failed synchronization can be retried later.
- Critical wake data is retained through transient backend outages.
- The next step is replacing Android prototype serialization with Room and adding reboot/timezone recovery.
