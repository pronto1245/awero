# AWERO Implementation Status

## 1. Product foundation
- [x] Product vision, JTBD, MVP scope and roadmap
- [x] Reliability-first / local-first principles
- [x] Successful Wake Rate as North Star

## 2. Repository / architecture
- [x] Monorepo structure
- [x] NestJS backend foundation
- [x] PostgreSQL schema foundation
- [x] API contracts foundation
- [x] Shared types
- [x] Localization package foundation
- [x] CI foundation
- [ ] Production CI build verification

## 3. Alarm engine
- [x] Alarm model
- [x] Versioned scheduling
- [x] Cancel/recreate on edit
- [x] Recurring weekdays
- [x] Device-local timezone
- [x] Fixed timezone
- [x] DST-aware wall-clock calculation
- [x] Test alarm
- [x] Pending schedule verification
- [x] Automatic repair
- [x] Android boot/time/timezone recovery
- [ ] Physical-device reliability validation
- [ ] iOS Xcode project/signing validation

## 4. Wake flow
- [x] Wake session model
- [x] Triggered → mission → completed state flow
- [x] Snooze state
- [x] Emergency stop state
- [x] Session persistence prototype
- [x] Android wake activity
- [x] iOS wake screen
- [ ] Production-grade OS snooze rescheduling
- [ ] Physical-device wake validation

## 5. Missions
- [x] Math mission logic
- [x] Steps runtime foundation
- [x] QR runtime foundation
- [x] Android ML Kit QR decoding
- [x] Mission fallback policy
- [ ] Mission UI for Math
- [ ] Mission UI for Steps
- [ ] Mission UI for QR
- [ ] Retry / timeout UX
- [ ] Runtime fallback transitions
- [ ] Photo AI mission (V2)
- [ ] Mixed mission (V2)

## 6. Local data
- [x] Prototype local alarm storage
- [x] Prototype wake-session storage
- [x] Statistics model
- [x] Adaptive policy
- [ ] Room production storage
- [ ] Core Data/SQLite production storage
- [ ] Persistent statistics/streak UI

## 7. Onboarding / localization
- [x] Anonymous first-run mode foundation
- [x] Permission education/runtime requests
- [x] First alarm flow foundation
- [x] Test alarm flow
- [x] Translation resource foundation exists for en, ru, pt-BR, fr, de, es
- [ ] Wire all user-facing iOS strings to localized resources for all six locales
- [ ] Wire all user-facing Android strings to localized resources for all six locales
- [ ] Complete and validate matching keys and fallback behavior across locales
- [ ] Verify system-language selection and localization tests on both platforms

## 8. Backend / sync
- [x] Auth session foundation
- [x] Anonymous account
- [x] Device registration
- [x] Alarm CRUD API
- [x] Wake session API (transactional events, idempotency, owner checks)
- [x] Statistics API (owner-scoped totals and streaks)
- [x] Analytics ingestion (owner-scoped batches and idempotent retries)
- [x] Offline sync queue (transactional ALARM create/update/delete reconciliation)
- [x] Conflict resolution (optimistic versions, stable retry results, server snapshot returned)
- [x] Support diagnostics tickets (bounded, owner-scoped, idempotent)

## 9. Monetization
- [ ] iOS StoreKit 2
- [ ] Android Google Play Billing
- [ ] Server entitlement validation
- [ ] Restore purchases
- [ ] Free/Pro gating
- [ ] Paywall
- [ ] Subscription expiry safety

## 10. AI
- [x] Basic local adaptive policy
- [ ] AI profile API
- [ ] AI recommendation API
- [ ] JSON schema validation
- [ ] Policy engine
- [ ] Recommendation feedback
- [ ] Explainable reason codes

## 11. QA
- [ ] Unit tests
- [ ] Integration tests
- [ ] E2E tests
- [ ] Offline tests
- [ ] Reboot tests
- [ ] Timezone/DST tests
- [ ] Permission-denied tests
- [ ] Subscription tests
- [ ] Physical iOS test
- [ ] Physical Android test

## Release gate

AWERO must **not** be described as production-ready until the physical iOS and Android release candidates pass the critical E2E matrix and the release checklist.

Current status: **MVP implementation in progress; repository contains foundation and runtime scaffolding, not a verified production build.**

## Competitor research
- [x] Public App Store / Google Play descriptions, screenshots and visible review excerpts reviewed for Erly, Alarmy, I Can't Wake Up!, WAKR and Wakey.
- [x] Competitor features and complaint-derived AWERO acceptance criteria recorded in [Competitor and Review Gap Analysis](research/COMPETITOR_AND_REVIEW_GAP_ANALYSIS.md) and [Product Requirements](PRODUCT_REQUIREMENTS.md).
- [ ] Expand review sampling systematically and test competitors in installed runtimes before treating the research as comprehensive. Current desk research is qualitative, based on public storefront material, and does not mark AWERO product code complete.

## Reliability Foundation

Status: CODE + CI VERIFIED

## Local Persistence Hardening

The approved ten-point **automated local persistence gate** has a complete successful CI baseline. Both platforms have crash/restart checks; Android additionally exercises real process SIGKILL and AlarmManager recovery. Core Data failure handling, Room 1→2 schema validation, sync retry deduplication, wake-flow restore and local E2E are covered.

The verification matrix, workflow evidence and physical-device boundary are recorded in [Local Persistence Hardening](LOCAL_PERSISTENCE_HARDENING.md). Backend work is excluded from this gate.

## Product requirements and competitor coverage

- [x] Governing cross-platform product specification and implementation order recorded in [AWERO Product Specification and Implementation Plan](AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md)
- [x] Detailed capability inventory retained in [Product Requirements](PRODUCT_REQUIREMENTS.md)
- [x] Erly, Alarmy, I Can't Wake Up!, WAKR and Wakey features/review signals mapped to AWERO acceptance criteria in [Competitor and Review Gap Analysis](research/COMPETITOR_AND_REVIEW_GAP_ANALYSIS.md)
- [x] Full feature inventory retained by stage: P0 reliability/core missions/localization; P1 exercise, photo/object, sequences, wake-up check and accurate progress; P2 optional weather briefing, sleep support, social accountability and AI/adaptive support
- [ ] Product capabilities implemented and verified; documentation does not mark these features as shipped

## Governing plan position
- [x] Phase 0 — reconcile scope, competitor inputs, phase order, and acceptance gates across product docs
- [x] Phase 1 — code-level iOS and Android alarm reliability with CI coverage; physical-device release checks remain deferred to Phase 10
- [x] Phase 2 — wake-session runtime (automated gate; see verification below)
- [ ] Phase 3 — P0 Math, Steps, and QR/barcode mission flows
- [ ] Phase 4 — local persistence CI/simulator checks (the ten-point automated CI gate is complete; physical-device checks are deferred to final release validation)
- [ ] Phase 5 — backend and sync isolated from the alarm-critical path
- [ ] Phase 6 — user-facing UX and six-language completion

## Phase 2 automated gate — complete

- Baseline `34932a904bae95069da485edf9cef45ac43fa5d7` passed all four jobs in [AWERO CI run 574](https://github.com/pronto1245/awero/actions/runs/37832150008).
- Commit `da3992ad810ce152a8445c0712a8dd049e4191f7` prevents overlapping iOS snooze requests from cancelling the successfully scheduled snooze. Two targeted local XCTest cases passed, followed by all four jobs in [AWERO CI run 575](https://github.com/pronto1245/awero/actions/runs/37833128379).
- Commit `28c1ae960d9515869b6c91101b8b6dc7dd93ffb5` makes Android wake UI render the restored controller state and selected mission, including a persisted Math fallback. Its emulator E2E re-delivers an alarm intent, checks the same session resumes, solves the fallback and verifies persisted success. All four jobs passed in [AWERO CI run 576](https://github.com/pronto1245/awero/actions/runs/37834366415).
- Commit `ee3e2df07473c42e6d583321b93ab2388453178e` makes iOS mission start, fallback and completion report persistence failures and offer a retry of the failed action. A failed write keeps the session active and does not advance mission/result state. Three targeted local XCTest cases using a read-only store passed, followed by all four jobs in [AWERO CI run 577](https://github.com/pronto1245/awero/actions/runs/37835695292).
- Android now also rejects overlapping snooze requests while the Room write is pending, preserving the successful OS schedule. A new emulator test sends the second tap during persistence, verifies one schedule/no cancellation and the saved count, then verifies another snooze after the next delivery. Commit `a325fab24ec97c6f5059d2ae547e6bcfd1949deb` passed all four jobs in [AWERO CI run 578](https://github.com/pronto1245/awero/actions/runs/37837964796).
- Backend/sync changes and physical-device release gates are outside this correction.

## Phase 3 — active

The first package addresses P0 Mission runtime / recovery requirements: two-minute Steps/QR timeout to local Math, manual fallback, camera/sensor cleanup when the mission view is detached, and retrying a failed save after a validated mission. Math remains the deterministic local fallback.

The next scoped package adds camera-based QR/barcode capture during alarm creation/editing on both platforms, an optional exact-content field, blank-code save prevention, persisted expected code and iOS Math difficulty selection. Android scans ML Kit-supported formats; iOS uses the supported subset of QR, EAN/UPC-E, Code 39/93/128, PDF417, Aztec and Data Matrix. Permission denial/camera failure during setup preserves the draft and offers manual entry. The iOS persistence/edited-code validation test passed locally; Android's existing restart test now includes the expected code.

Phase 3 remains active: Android now has an instrumentation case proving a test alarm can complete its persisted wake session without changing planned/completed wake statistics; CI verification is pending. Offline and remaining permission/retry mission E2E cases still require implementation/verification. Camera decoding on physical devices remains a Phase 10 check. Full localization stays in Phase 6.

### User-reported iPhone walkthrough

On 2026-10-08, the user confirmed that the guided iPhone walkthrough worked. The walkthrough covered creating a QR alarm, scanning the saved code at wake-up, invalid-code fallback to Math, Steps, and snooze. This is a manual report without recorded device/OS details; it does not replace the broader Phase 10 release matrix.
