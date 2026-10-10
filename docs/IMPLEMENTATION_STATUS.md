# AWERO Implementation Status

**Current governing plan:** [AWERO Product Specification and Implementation Plan](AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md) (re-baselined 2026-10-09). This file records evidence and gaps; it is not a second roadmap.

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
- [x] Mission UI for Math
- [x] Mission UI for Steps
- [x] Mission UI for QR
- [x] Retry / timeout UX
- [x] Runtime fallback transitions
- [ ] Photo AI mission (V2)
- [ ] Mixed mission (V2)

## 6. Local data
- [x] Prototype local alarm storage
- [x] Prototype wake-session storage
- [x] Statistics model
- [x] Adaptive policy
- [x] Room local persistence and schema migration path
- [x] Core Data local persistence path
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

These checks describe backend endpoint/storage capabilities. They do **not** mean mobile clients have complete end-to-end flows for every endpoint. Client integration is tracked separately below; full alarm download/reconciliation, wake-session delivery, and diagnostics submission remain incomplete.

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
- [x] Automated unit, integration, Android instrumentation, iOS simulator, offline, persistence, and selected recovery scenarios run in CI (see phase-specific evidence below)
- [ ] Full physical-device P0 release matrix on both platforms
- [ ] Store purchase tests (pending Phase 7)

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

## Phase 0 — re-baseline and architecture contract

Phase 0 was previously recorded in `c0aa848`, reopened by the owner on 2026-10-09, and fully re-audited and closed on 2026-10-09. The approved plan is [AWERO Product Specification and Implementation Plan](AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md); this file is the only status matrix. Phase 0 closure evidence is the green CI attached to the commit that records this closure; no later phase is included in that commit.

- [x] 1. Consolidate the supplied audit, repository evidence, CI history, and user-reported device checks into the governing plan and this matrix. The supplied audit was taken at `785093a`; the recorded audit baseline at that time was `64b5061`. Confirmed findings and their phase owners are listed below.
- [x] 2. Record current and target sources without claiming incomplete packages are wired: product rules in `docs/PRODUCT_REQUIREMENTS.md`; PostgreSQL schema in migrations; target OpenAPI in `packages/api-contracts/openapi.yaml` (not created before Phase 5); target shared types in `packages/shared-types` (currently unused by production clients); target locale source in `packages/localization` (11–14 keys versus 129 iOS/124 Android native strings, with generation in Phase 6); AI schemas remain policy documentation until Phase 9.
- [x] 3. Inventory confirmed duplicate sources and production stubs and assign them to phases. Remove duplicate roadmap files and nonexistent workspace entries. Specific confirmed duplication: Android `MissionType` exists in alarm and mission packages; fallback engines on iOS/Android are unused while active wake controllers implement Math fallback directly (Phase 3); Android has Compose and View-based wake presentations (consolidate shared flow/presentation in Phase 6 while retaining the native alarm Activity); mission/difficulty enums and request/response shapes are copied across native apps, API, and unused shared packages (contract/model parity in Phase 5); alarm DTO constraints and SQL projections repeat in backend controllers (Phase 5, backend untouched before then). Runtime keyword scan found no confirmed production mock/demo path; it is not proof of behavioral completeness, so each implementation slice still inspects its actual path. Platform-specific OS adapters are not duplicates to delete.
- [x] 4. Preserve native SwiftUI/AlarmKit/UserNotifications and Kotlin/Compose/AlarmManager adapters; no rewrite, parallel project, or replacement copy is authorized.
- [x] 5. Correct documentation naming nonexistent repository components and assign build/release debt to its phase below.

**Phase 0 acceptance:** all five items above and the governing plan exit gate are complete. The owner explicitly requested full Phase 0 closure. The final closure commit must pass its single associated CI run; prior green device/platform checks remain accepted and are not repeated. This closure does not authorize implementation work from later phases.

Before the Phase 0 closure commit, code CI [37954587946](https://github.com/pronto1245/awero/actions/runs/37954587946) passed for `eb10944`; code HEAD `237201a` had the identical tree (`664378000eb1f87fbf322f48714217565468df9f`) after the out-of-plan label change was reverted. The Phase 0 documentation/configuration baseline `8852345` passed all four jobs in [CI run 37975465395](https://github.com/pronto1245/awero/actions/runs/37975465395). The final closure commit receives its own one-time CI check. Existing device and green CI evidence is retained, not repeated.

## Governing plan position
- [x] Phase 1 — **complete on 2026-10-09**: P0 launch and alarm reliability. iOS storage-read errors are surfaced with loading/error/retry state (`bb0f04b`); Android audio failure falls back to an alarm tone and vibration (`8dbc032`), both passed CI [37946349952](https://github.com/pronto1245/awero/actions/runs/37946349952). Android uses `setAlarmClock` (`aa430b0`, all jobs green in [37948061694](https://github.com/pronto1245/awero/actions/runs/37948061694)); Direct Boot schedule restoration (`b5ff749` plus `e5f4912`) passed [37951703595](https://github.com/pronto1245/awero/actions/runs/37951703595). iOS weekday notification scheduling/rollback passed focused XCTest in [37953899030](https://github.com/pronto1245/awero/actions/runs/37953899030). The reported iPhone blank/partial-screen/startup issue was fixed by `acc7fa6`; its CI [37934795014](https://github.com/pronto1245/awero/actions/runs/37934795014) passed, and the user confirmed the app works on the iPhone; preserve that confirmation and do not repeat the walkthrough. Commit `20ca506` gives each simultaneous Android alarm independent playback, fallback tone, notification/PendingIntent identity, and targeted stop; the added test plus Android build/unit/instrumentation passed in [CI run 37980721063](https://github.com/pronto1245/awero/actions/runs/37980721063), whose four jobs are green. iOS notification fallback explicitly reports that Silent Mode or Focus may limit sound. Simulator/CI does not claim physical audible-delivery validation; that remains part of Phase 10.
- [x] Phase 2 — **complete on 2026-10-10**. Test sessions are tagged and excluded from real statistics/history; persisted snooze, mission/fallback and completion state restore after controller/process recreation; Android supports concurrent independent sessions and rejects duplicate deliveries; iOS snooze re-delivery and AlarmKit Stop→mission are covered. See the Phase 2 closure below. Android/iOS target jobs passed in [CI run 37990744262](https://github.com/pronto1245/awero/actions/runs/37990744262). `repository-check` was blocked before code checkout by Docker Hub's unauthenticated PostgreSQL image-pull rate limit; no backend or sync files changed in this phase.
- [x] Phase 3 — **complete on 2026-10-10**. Automated Math/Steps/QR runtime, fallback, offline, timeout, permission-denial, retry, duplicate-delivery, and test-alarm-statistics gates pass. Final platform CI passed in [run 37994679758](https://github.com/pronto1245/awero/actions/runs/37994679758): Android build/unit/instrumentation, iOS build/typecheck/smoke/wake-flow XCTest, and iOS persistence smoke are green. `repository-check` failed before checkout while pulling its PostgreSQL Docker image; it did not run or indicate a Phase 3 code failure. The user-confirmed iPhone walkthrough remains accepted and was not repeated. Physical Android and broader release validation remain Phase 10.
- [x] Phase 4 — **complete on 2026-10-10**. iOS Core Data reads now distinguish an empty result from store/read/decode failure across alarms, wake sessions, statistics, analytics, sync operations and conflicts. iOS and Android show an explicit localized alarm-storage error with retry; schedule recovery does not run from a failed read. Legacy alarm and wake-session migrations remain retryable after missing/corrupt data or database failure; statistics migration does not reimport a stale legacy snapshot. Focused tests cover corrupt-store versus empty-store behavior, migration retry, and preservation of migration state. CI [37999302184](https://github.com/pronto1245/awero/actions/runs/37999302184) passed all four jobs: repository checks, Android build/unit/emulator instrumentation, iOS build/typecheck/wake-flow XCTest, and iOS persistence/restart smoke. The previously accepted ten-point persistence baseline and the user-confirmed iPhone walkthrough were not separately repeated. Local backup-loss limits are documented; physical backup/reinstall remains Phase 10. Backend files were not changed.
- [ ] Phase 5 — **current; implementation in progress, production acceptance open**: commit series `eb3ca39`–`cb0675f` replaces `deviceId` ownership with a random installation credential, preserves existing valid sessions, adds registration throttling and request protections, isolates bad sync operations, handles foreign alarm IDs as per-item rejections, bounds event dates, adds owner indexes, and moves API persistence into services with one shared alarm policy/projection. Both apps contain HTTPS-only auth, outbound alarm/wake/analytics/diagnostics paths, inbound alarm reconciliation and persisted conflict protection. All four jobs passed in [CI run 38026854386](https://github.com/pronto1245/awero/actions/runs/38026854386), including PostgreSQL migration/API smoke, Android build/unit/emulator persistence tests, iOS build/typecheck/wake-flow tests and iOS persistence smoke. The public HTTPS URL remains unset because no domain was supplied, so app-to-live-API round trips cannot yet be accepted. Canonical OpenAPI/client-model parity remains outstanding. Conflicts are persisted and protected from overwrite; a user-facing resolution flow remains assigned to the UX phase.
- [ ] Phase 6 — **acceptance validation in progress**. The implementation, six-locale generation/parity, edit/cancel/back, delete confirmation/recovery, and localized permission/error paths are present. However, prior CI run [38029529137](https://github.com/pronto1245/awero/actions/runs/38029529137) did not exercise first-run onboarding through the actual UI. This change adds Android emulator and iOS simulator UI tests for the accessible onboarding title/button and the transition to alarm creation. Close the phase only after those tests pass in CI. Physical VoiceOver/TalkBack operation remains in the Phase 10 device matrix; the user-confirmed iPhone alarm walkthrough is preserved and will not be repeated.
- [ ] Phase 7 — **not started**: billing and trial eligibility. No current trial ledger exists. New anonymous registrations must never mint/reset a trial; store eligibility and server-side trial history must agree.

## Cross-phase acceptance rule

Every implementation slice uses real production data paths: native OS APIs and local persistence for offline alarm/wake behavior, and a live configured HTTPS API for remotely backed features. Production screens must not contain mock responses, seeded demo records, fabricated statistics, or fake success states. Mocks/fixtures are allowed only in test targets. Empty, offline, permission-denied, and service-error states must be truthful. This rule applies immediately in every phase. Do not enable a production API until its auth and abuse controls pass Phase 5; a missing URL must remain an explicit unavailable state, not simulated data.

Both mobile sync configurations currently return “not configured” when no HTTPS base URL is supplied. The repository scan found test fixtures but no obvious production code tagged `mock`, `stub`, `fake`, `demo`, or `placeholder`; that text scan does not prove every runtime path is real. Inspect behavior in each feature slice and keep remote functionality explicitly unavailable until its live service is configured. The user has not yet supplied a public API domain.

## Consolidated audit findings and ownership

The supplied audit is dated 2026-10-09 and names commit `785093a`; its repository baseline at audit time was `64b5061`. The inspected API/security and alarm files relevant to its critical claims were unchanged between those two commits. Findings below are assigned to the governing plan rather than repeated as another independent plan.

- **Phase 1 — alarm and launch:** complete. Android independent simultaneous ringing is implemented in `20ca506`: each alarm/test identity has its own player and fallback tone, notification/PendingIntent identity and stop command; no active alarm stops another. The targeted identity test and Android assemble/unit/instrumentation jobs passed in `37980721063`. Android sound failure fallback/vibration is implemented in `8dbc032` and passed CI run `37946349952`; `aa430b0` publishes schedules via `setAlarmClock` and passed all jobs in `37948061694`. Direct Boot stores only schedule fields in device-protected storage and restores them on `LOCKED_BOOT_COMPLETED`; `b5ff749` plus `e5f4912` passed all CI jobs in `37951703595`. On iOS notification fallback, the scheduler verifies all selected weekdays remain pending, preserves already-scheduled alarms if iOS silently drops a new request, and returns a localized error for an incomplete schedule; focused XCTest passed in `37953899030`. This guard does not assume an undocumented fixed capacity. iOS pre-26/fixed-timezone fallback still uses ordinary `.default` notifications and truthfully reports that Silent Mode or Focus may limit sound. The iPhone blank/partial-screen/startup issue was fixed by `acc7fa6`, CI `37934795014` passed, and the user confirmed the app works on the iPhone; preserve this evidence without repeating the walkthrough. Physical audible-delivery validation remains in Phase 10.
- **Phase 2/4 — wake and local data:** Phase 2 wake-session integrity is complete as recorded below. Core Data fetch errors surface as a distinct error/retry state in `bb0f04b`, validated by CI run `37946349952`. Never repeat the accepted emergency-stop or full manual iPhone walkthrough tests unless that exact behavior changes.
- **Phase 3/6 — historical baseline and ownership:** the audit found an unwired `OnboardingScreen`, non-saveable Android edit state, missing delete recovery, and localization/accessibility gaps; Phase 6 completion above records those UX fixes. Android's duplicate `MissionType` and unused platform `FallbackEngine` copies were assigned to Phase 3 and are recorded there. Native alarm/OS adapters remain.
- **Phase 5 — backend security/integration:** the baseline findings about `deviceId` takeover, batch-wide rollback, foreign alarm primary-key failure, missing request controls, timestamp bounds, indexes, and absent mobile send/pull paths are addressed in the current implementation slice. Verification and public production exposure remain gated on PostgreSQL/mobile CI and a configured public HTTPS origin. Conflicts are kept locally and their alarm IDs are protected during pulls; user-facing resolution is tracked with UX work.
- **Phase 5/7 — identity and trial:** anonymous registration now requires a high-entropy installation credential and never returns or grants a trial/entitlement. That credential binds an app installation; `deviceId` remains metadata and cannot prove ownership. Trial eligibility/history and verified StoreKit/Play Billing state are not implemented yet and remain Phase 7. The Phase 7 design must use server-verified store eligibility/history so a new anonymous account cannot mint or reset a trial; a client-generated installation ID alone cannot prove a physical device survived reinstall.
- **Architecture cleanup — assigned by phase:** CRUD/sync repeat alarm SQL projections, mapping, DTO constraints, and controller-owned persistence (Phase 5). Mission/difficulty enum copies exist in Kotlin, Swift, backend, and the currently unused `packages/shared-types`; neither shared-types nor api-contracts is imported by production clients (API contract/model parity belongs to Phase 5). The 11–14-key JSON catalogs and manually maintained native resources were the pre-Phase-6 baseline; Phase 6 now has complete JSON sources and generated native resources. Android retains a View-based `WakeAlarmActivity` with its native OS presentation and a Compose wake screen; preserve those platform roles. The unused `FallbackEngine` copies duplicate active Math fallback policy (Phase 3). `pnpm-workspace.yaml` listed nonexistent `apps/worker` and `apps/admin`; those entries are removed in the Phase 0 closure. README/ADR claims have been corrected and redundant roadmap files removed. No `pnpm-lock.yaml`, Gradle wrapper, or `PrivacyInfo.xcprivacy` was found: reproducible dependency/build setup and store/privacy assets are assigned to Phase 10. API scripts name Jest/ESLint without declaring them; backend unit/security coverage is Phase 5 and CI/toolchain cleanup is Phase 10. A production-path keyword scan found no confirmed `mock`/`stub`/`fake`/`demo`/`placeholder` implementation; this is not a behavioral guarantee, so each feature slice must verify its live path.

The estimate of 27–38 developer days in the supplied report is a planning estimate, not a source-verifiable fact. Platform-specific claims such as the exact iOS pending-notification cap and AlarmKit recovery after closing the app need narrowly scoped platform confirmation; they are not grounds to repeat the completed iPhone mission walkthrough.

## Previously accepted evidence and boundaries

- Green CI proves only the jobs listed in that run; it does not close missing client/API flows or device gates.
- The governed phase table is in [AWERO Product Specification and Implementation Plan](AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md). Historical CI notes below document what was tested; they do not override the current phase gates.
- The user-confirmed iPhone walkthrough/manual checks are accepted as completed. The source audit identified code-level gaps but does not authorize repeating the same manual checks.
- Remaining product scope includes billing, P1 progress/history and missions, P2 weather/sleep/social/AI, and the broader Phase 10 release matrix.
- Never reset or stage unrelated dirty files/commits. Each scoped change is committed and pushed immediately; verify the resulting CI once and proceed without rerunning unchanged green suites.

## Phase 2 automated gate — complete

### Phase 2 final closure — 2026-10-10

Commits `763b1fa`, `521bfbb`, `ebd506b`, `19c25e3`, `4ec9bbf`, and `dc8625f` close the remaining P0 wake-session gaps. Test/real alarm identity is persisted through Room/Core Data and OS redelivery; test sessions do not write real wake statistics and are excluded from normal history. Android supports simultaneous active wake sessions for distinct alarms (including test/real sessions for the same alarm), serializes state transitions, makes duplicate trigger delivery idempotent, and restores snooze/mission/fallback state. iOS restores snooze count and ringing state, prevents overlapping snooze scheduling from cancelling the successful request, resumes persisted mission/fallback state, and routes AlarmKit Stop into the mission on the same saved session. A failed snooze or persistence write does not falsely advance the session and remains retryable. No backend or sync code changed.

Targeted verification on final code commit `dc8625f17c9754ec3dd6b21a88b05de35fd410bc` is in [CI run 37990744262](https://github.com/pronto1245/awero/actions/runs/37990744262): Android assemble, unit tests and all 28 emulator instrumentation tests passed; iOS simulator build, configuration validation, Swift typecheck, scheduler smoke, and wake-flow XCTest passed; Core Data persistence smoke including forced process termination passed. The repository job did not reach checkout/tests because Docker Hub rate-limited the unauthenticated `postgres:16-alpine` pull during container initialization. This is unrelated to Phase 2 code; backend was unchanged and its applicable prior green repository check remains recorded above. Do not claim this run as an all-jobs-green workflow.

The user-confirmed iPhone walkthrough and the earlier emergency-stop test remain accepted evidence and were not repeated. The acceptance gate is automated on each platform and includes local/offline wake paths; physical release-matrix checks remain Phase 10.

- Baseline `34932a904bae95069da485edf9cef45ac43fa5d7` passed all four jobs in [AWERO CI run 574](https://github.com/pronto1245/awero/actions/runs/37832150008).
- Commit `da3992ad810ce152a8445c0712a8dd049e4191f7` prevents overlapping iOS snooze requests from cancelling the successfully scheduled snooze. Two targeted local XCTest cases passed, followed by all four jobs in [AWERO CI run 575](https://github.com/pronto1245/awero/actions/runs/37833128379).
- Commit `28c1ae960d9515869b6c91101b8b6dc7dd93ffb5` makes Android wake UI render the restored controller state and selected mission, including a persisted Math fallback. Its emulator E2E re-delivers an alarm intent, checks the same session resumes, solves the fallback and verifies persisted success. All four jobs passed in [AWERO CI run 576](https://github.com/pronto1245/awero/actions/runs/37834366415).
- Commit `ee3e2df07473c42e6d583321b93ab2388453178e` makes iOS mission start, fallback and completion report persistence failures and offer a retry of the failed action. A failed write keeps the session active and does not advance mission/result state. Three targeted local XCTest cases using a read-only store passed, followed by all four jobs in [AWERO CI run 577](https://github.com/pronto1245/awero/actions/runs/37835695292).
- Android now also rejects overlapping snooze requests while the Room write is pending, preserving the successful OS schedule. A new emulator test sends the second tap during persistence, verifies one schedule/no cancellation and the saved count, then verifies another snooze after the next delivery. Commit `a325fab24ec97c6f5059d2ae547e6bcfd1949deb` passed all four jobs in [AWERO CI run 578](https://github.com/pronto1245/awero/actions/runs/37837964796).
- Backend/sync changes and physical-device release gates are outside this correction.

## Phase 3 — complete on 2026-10-10

The first package addresses P0 Mission runtime / recovery requirements: two-minute Steps/QR timeout to local Math, manual fallback, camera/sensor cleanup when the mission view is detached, and retrying a failed save after a validated mission. Math remains the deterministic local fallback.

The next scoped package adds camera-based QR/barcode capture during alarm creation/editing on both platforms, an optional exact-content field, blank-code save prevention, persisted expected code and iOS Math difficulty selection. Android scans ML Kit-supported formats; iOS uses the supported subset of QR, EAN/UPC-E, Code 39/93/128, PDF417, Aztec and Data Matrix. Permission denial/camera failure during setup preserves the draft and offers manual entry. The iOS persistence/edited-code validation test passed locally; Android's existing restart test now includes the expected code.

Android instrumentation verifies test alarms stay out of wake statistics, the alarm and mission flow works with emulator networking disabled, a validated result can be retried after a failed write, and denied camera permission persists a Math fallback that can complete successfully. These gates passed in [CI run 37844092499](https://github.com/pronto1245/awero/actions/runs/37844092499), [CI run 37877786906](https://github.com/pronto1245/awero/actions/runs/37877786906), and [CI run 37881243655](https://github.com/pronto1245/awero/actions/runs/37881243655), all four jobs green. The user also confirmed the guided QR, invalid-code fallback, Math, Steps and snooze walkthrough works on iPhone. Physical Android coverage and the broader release matrix remain Phase 10 checks. Full localization stays in Phase 6.

The final Phase 3 slice removes unused duplicate mission/fallback engines and the duplicate Android mission enum, removes fake validators in favor of the active QR scanner callback, and routes fallback decisions through one policy per platform. Focused tests cover Math answer validation/difficulty bounds, exact QR payload matching, fallback selection, and Android step-counter zero-baseline/regression behavior. The final platform run passed Android assemble/unit/instrumentation tests and iOS simulator build/typecheck/scheduler smoke/wake-flow XCTest in [CI run 37994679758](https://github.com/pronto1245/awero/actions/runs/37994679758); iOS persistence smoke also passed. Its unrelated `repository-check` failed before checkout during PostgreSQL Docker image initialization, so backend code did not run or change. The accepted iPhone walkthrough was not repeated.

### User-reported iPhone walkthrough

On 2026-10-08, the user confirmed that the guided iPhone walkthrough worked. The walkthrough covered creating a QR alarm, scanning the saved code at wake-up, invalid-code fallback to Math, Steps, and snooze. This is a manual report without recorded device/OS details; it does not replace the broader Phase 10 release matrix.

## Phase 5 — security and live-sync implementation record

Earlier Phase 5 API behavior remains covered by the accepted runs [37882395259](https://github.com/pronto1245/awero/actions/runs/37882395259) and [37887632818](https://github.com/pronto1245/awero/actions/runs/37887632818). The current security/sync implementation passed all four CI jobs in [run 38026854386](https://github.com/pronto1245/awero/actions/runs/38026854386). Local API typecheck/build, smoke-script syntax, Swift Core typecheck/syntax, and whitespace checks also passed. The public API URL is intentionally unset because no domain has been supplied, so public HTTPS round trips from app clients remain unverified and no production endpoint is simulated. Canonical OpenAPI/client-model parity is also not yet implemented. Phase 5 cannot be recorded as 100% until those items are complete. Phase 6 and Phase 7 retain their assigned work.


## Phase 6 — historical UX implementation record

The first UX/localization slice updates the iOS and Android home screens to the approved warm ivory, navy and coral design. It selects the next enabled alarm using each alarm’s configured timezone, shows repeat days and mission, supports enable/disable, and explains that the first alarm works offline without an account. Home-screen copy and actions now have matching keys in English, Russian, Brazilian Portuguese, French, German and Spanish; the locale parity check covers those keys. Alarm setup, permission education, mission and notification copy are implemented. Home and mission text now respect system time formats and Dynamic Type; create/edit screens expose device-following or fixed time-zone behavior. Settings is reachable from the approved gear control on both platforms, shows the current device language, and opens the phone’s app-permission settings.


The second Phase 6 slice completes the alarm setup flow on both platforms: repeat-day selection, localized mission/difficulty controls, QR setup guidance, and permission explanations. iOS now shows its notification-permission explanation before the first system request instead of requesting access at app launch. Android persists selected repeat days and offers Settings when required access blocks saving. Setup copy has exact six-locale key parity.


The localized alarm-home and alarm-setup slices passed all four jobs in [CI run 37912583144](https://github.com/pronto1245/awero/actions/runs/37912583144). A Phase 6 slice translated the wake controls and Math, Steps and QR mission prompts across both apps. Its acceptance is tracked by the six-locale parity check and the platform CI.


The home, setup and wake/mission packages passed all four CI jobs in [run 37915377895](https://github.com/pronto1245/awero/actions/runs/37915377895). This Phase 6 package wires localized notification contents and schedule/permission errors into both platform runtimes.


The historical Phase 6 accessibility/localization slices added the iOS alarm VoiceOver label, localized stopped-state title, settings entry and QR preview description. Commits [a00aac7](https://github.com/pronto1245/awero/commit/a00aac7726cbf0a3cf7102ce7a9ec4fa1d654227) and [49c1d35](https://github.com/pronto1245/awero/commit/49c1d357edc7f4e55457b007c5e3e132c01f73d4) passed all four jobs in [CI run 37920473384](https://github.com/pronto1245/awero/actions/runs/37920473384). The previously open onboarding, edit/delete, and canonical-locale gaps are closed in the Phase 6 completion entry above.

The latest accessibility/localization packet adds the approved gear entry and a localized Settings screen on iOS and Android. The screen identifies the device language and links to app permissions; its labels and settings copy have exact parity in all six locales. The QR camera preview now has a localized screen-reader description, and the primary wake and mission text scales with Dynamic Type. Commit [2f08649](https://github.com/pronto1245/awero/commit/2f08649b57232138dce73825177a43eb3bfe2c43) passed all four jobs in [CI run 37928994590](https://github.com/pronto1245/awero/actions/runs/37928994590).


The user reports the iPhone walkthrough and manual checks are complete; do not ask for or repeat that work. Phase 6 code and CI closure is recorded above. Manual VoiceOver/TalkBack use and the full physical alarm reliability matrix remain in Phase 10; do not repeat the confirmed iPhone alarm walkthrough.
