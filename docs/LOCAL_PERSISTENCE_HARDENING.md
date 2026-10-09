# Local Persistence Hardening

Scope: the approved ten-point local persistence gate. Backend is excluded.

Status: automated gate verified on 2026-10-08. Physical-device release validation remains separate.

## Data-loss boundary

Alarms, wake sessions, statistics, and pending offline operations are stored locally on each device. The current product does not provide a server restore path for this local state. If application data is removed and the operating system does not restore it from a device backup, AWERO cannot recover those records. A successful local migration preserves the records on that device; a failed migration remains retryable and is shown as a storage error rather than as an empty alarm list. Physical backup and reinstall behavior remains part of the Phase 10 release matrix.

## Automated verification matrix

| # | Requirement | Verification |
|---|---|---|
| 1 | Crash / restart | iOS: committed SQLite data survives SIGKILL. Android: instrumentation writes alarm, active session, statistics and queued retry; CI kills the target application process while Room remains open; a new process reads and verifies the data. A regression test also verifies that a missing statistics migration marker cannot overwrite committed Room counters with stale legacy data. |
| 2 | Alarm restore | Both platforms reload saved alarms. Android verifies AlarmManager requests survive process termination, cancels and repairs missing schedules, and removes disabled schedules. iOS reconciles native notification request identifiers, calendar fields and timezones using an injected notification center; verifies stale-version replacement, disabled-alarm cancellation and retry after scheduling failure. |
| 3 | Duplicate Sync Queue / retry after crash | Repeated operation IDs remain unique. Retry deadline and attempt count survive process termination; a duplicate enqueue preserves retry metadata. Acknowledgment removes the operation. |
| 4 | WakeSession idempotency | Repeated start/mission/complete transitions do not duplicate sessions or events. iOS additionally tests concurrent triggers and completions. Android preserves the event sequence through process restart. |
| 5 | Core Data save failures | A read-only SQLite store rejects writes. Failed alarm/session/statistics writes do not publish unsaved state or mutate notification schedules. Failed statistics migration does not set the completion marker. |
| 6 | Room migration 1 → 2 | Instrumentation opens a genuine Room v1 database, validates migration against the v2 entity schema, preserves alarm/session data and verifies the wake_events table and index. WakeEventEntity declares the same index created by MIGRATION_1_2. |
| 7 | iOS migration | Smoke test opens a SQLite store created with the earlier alarm/statistics/session model, migrates it with Core Data and verifies existing alarm values. |
| 8 | Persistence tests | CI runs Android build, Robolectric unit tests, real-emulator instrumentation, explicit cross-process SIGKILL phases, iOS Xcode project generation and simulator build, Swift typecheck and persistence smoke. Android reports and crash logs are retained as workflow artifacts. |
| 9 | UI Wake Flow restore | Controllers reconstruct mission state, alarm, fallback mission and snooze count from persistent data. iOS tests failed-write transitions leave UI state unchanged. |
| 10 | Final local E2E | Alarm → wake session → mission → process restart → restored flow → completion → reopened statistics; duplicate completion does not increase counters. iOS also verifies emergency-stop persistence. |

## Test entry points

- `.github/workflows/ci.yml`
- `apps/ios/Tests/PersistenceSmokeMain.swift`
- `apps/android/app/src/test/java/app/awero/core/PersistenceTest.kt`
- `apps/android/app/src/test/java/app/awero/PersistenceRecoveryTest.kt`
- `apps/android/app/src/androidTest/java/app/awero/core/storage/AweroDatabaseMigrationTest.kt`
- `apps/android/app/src/androidTest/java/app/awero/core/storage/PersistenceRecoveryTest.kt`
- `apps/android/app/src/androidTest/java/app/awero/core/storage/ProcessCrashRecoveryTest.kt`
- `apps/android/scripts/persistence-crash-test.sh`

## Validation boundary

This gate covers automated local persistence and controller-level E2E. iOS CI generates and builds the installable app for the simulator; notification reconciliation uses an injected center and does not prove notification delivery on a physical iPhone. Android uses a real emulator AlarmManager and actual process SIGKILL; force-stop, device reboot, OEM power restrictions and physical-device wake delivery remain release-checklist items. No backend integration or production-ready claim is implied.

## CI evidence

The first complete successful gate is commit `542459b248ebf0f7fc84ecc9e16ddfd06fc5e4a2`:

https://github.com/pronto1245/awero/actions/runs/37755400985

All four jobs finished with `success`: repository-check, android-build, ios-syntax and ios-persistence-smoke. The Android job includes the explicit cross-process read phase, which reports `OK (1 test)` and `AWERO Android SIGKILL persistence + alarm recovery + E2E: PASS`.

Later commits, including the additional missing-marker regression, must retain the same complete gate. Current main results:

https://github.com/pronto1245/awero/actions/workflows/ci.yml?query=branch%3Amain
