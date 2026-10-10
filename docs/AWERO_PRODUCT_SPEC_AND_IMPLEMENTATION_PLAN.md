# AWERO Product Specification and Implementation Plan

**Status:** Governing system plan; product code is not declared production-ready.

**Platforms:** iOS and Android.  
**Baseline date:** 2026-10-09.
**Owner:** AWERO product owner.  
**Research:** [Competitor and Review Gap Analysis](research/COMPETITOR_AND_REVIEW_GAP_ANALYSIS.md).  
**Feature inventory:** [Product Requirements](PRODUCT_REQUIREMENTS.md).  
**Implementation status:** [Implementation Status](IMPLEMENTATION_STATUS.md).  
**Persistence gate:** [Local Persistence Hardening](LOCAL_PERSISTENCE_HARDENING.md).

This is the **only governing implementation plan**, approved by the product owner on 2026-10-09. The attached plan has been incorporated here so this file is the repository source of truth. The only status matrix is [Implementation Status](IMPLEMENTATION_STATUS.md). Other documents may define product requirements, architecture details, research, or checklists, but they may not introduce another phase order or authorize work outside the active phase and its listed subpoints. Execute subpoints in order; everything outside the active phase is prohibited until its acceptance gate is met. Phases 0–4 are complete; Phase 5 is current as recorded in the status matrix.

A capability is not complete because a model, API, screen mock, or CI job exists: it must satisfy its acceptance criteria on both platforms. Retain the user's existing device evidence and already-green CI results. Do not repeat completed checks unless the associated behavior changes.

### Cross-phase implementation rule: real data and services

This rule applies to every phase, not as a later cleanup task. Production app flows must not use placeholder behavior, seeded demo records, fake API responses, or fabricated success. Use the real platform API and local persistence for local alarm/wake behavior; connect remotely backed features to a live, configured HTTPS API and real data in the same implementation slice. A missing production URL or provider credential is an explicit unavailable state, never a reason to show fake success or sample data; the core alarm remains local and usable. Mocks and fixtures are allowed only in test targets. Empty, offline, permission-denied, and service-error states must be explicit and truthful. A feature is not accepted while its production path still depends on a stub or mock.

## 1. Product definition

AWERO is a reliable alarm and wake-up app. The alarm must work locally and remain usable without registration, internet, backend, AI, or purchase. Missions, routines, progress, optional weather, sleep support, and social features extend the alarm; none may interfere with a scheduled alarm or prevent the user from stopping it safely.

The product is for people who need a dependable alarm and may benefit from a task that gets them out of bed. AWERO should match useful competitor capabilities and address observed failure reports through explicit, testable behavior. It cannot promise protection against operating-system actions that apps cannot prevent, such as the user powering off the phone.

## 2. Product invariants

1. A saved alarm has a truthful state: scheduled, permission/action required, or failed. A push notification or successful build is not proof of audible alarm delivery.
2. Alarm-critical scheduling and wake-session state are local-first. Offline, sign-in failure, backend failure, AI failure, subscription state, ads, analytics, and review prompts cannot block ringing, mission completion, snooze, emergency stop, or recovery.
3. Alarm time, repeat days, timezone behavior, sound, mission, next fire time, and required permissions are clear before the user relies on the alarm. Edits are deliberate; stale schedules are cancelled.
4. Every mission has clear instructions, retry rules, bounded completion criteria, timeout behavior, and an accessible recovery path. A camera/sensor/model/permission failure is a technical failure, never a user failure.
5. Test alarms never count as a completed wake. Statistics and streaks derive only from persisted wake-session events.
6. All user-facing text is localized in English, Russian, Brazilian Portuguese, French, German, and Spanish. This includes onboarding, alarm setup, mission instructions, errors, permission explanations, notifications, settings, help, and purchase terms.
7. Weather, location, sleep, health data, and social sharing are optional and consent-based. Optional services cannot become prerequisites for alarms.
8. App behavior and marketing must reflect real iOS and Android limits. Platform differences require a documented equivalent user outcome, not a false promise of identical implementation.
9. App registration is not a trial grant. A user/device cannot receive a fresh trial by reinstalling, clearing app data, or creating another anonymous registration. Trial eligibility and history are enforced server-side and, for store subscriptions, checked with the relevant store.
10. An app-provided installation ID is a lookup key, never proof of device ownership or a secret. Anonymous registration must not let a caller take over an existing account or invalidate its other valid sessions.
11. Each shared business rule, API contract, schema, and translation key has one maintained source of truth. Platform-specific UI and operating-system adapters may differ; copied business rules and hand-maintained contract/locale mirrors are removed as the affected slice is implemented.

## 2.1 Architecture rules and cleanup policy

- Keep the current native iOS/Android apps and NestJS/PostgreSQL backend. Do not rewrite the repository or introduce parallel projects.
- Use one versioned OpenAPI contract as the source for remote request/response shapes and error codes; generate or mechanically validate the Swift/Kotlin client models against it. Keep platform scheduling, sensors, UI, and persistence adapters native.
- Keep alarm domain validation and API-to-domain mapping in one backend service/repository path. Controllers handle HTTP concerns; do not copy alarm projections, ownership checks, or field validation between CRUD and sync endpoints.
- Keep locale JSON as the maintained translation source and generate native iOS/Android resources from it. Do not hand-maintain a second set of message values in source code. Locale-specific translations are expected; duplicated keys/values within a locale are not.
- Reuse shared types/contracts/localization packages by wiring them into the actual build and CI. Remove an unused abstraction only if the canonical source is moved and verified in the same scoped change; do not leave decorative packages.
- Do not delete platform-specific code merely because both platforms implement the same user feature. Remove duplicated policy and copied data definitions while retaining the native adapter needed for each OS.
- For each slice, identify production stubs, fake/demo data, and duplicate rules in the touched path; replace them with real persistence/platform/API behavior before acceptance. Keep test doubles inside test targets.
- Avoid broad cleanups unrelated to the active phase. Every change remains a small reviewable package, committed and pushed immediately, followed by its CI result.

## 2.2 Canonical sources of truth

- **Product and cross-platform business rules:** `docs/PRODUCT_REQUIREMENTS.md`. Native iOS/Android code remains the platform adapter; do not copy platform-independent policy into separate implementations without shared tests or validation.
- **Remote API contract (target):** one versioned OpenAPI document owned by `packages/api-contracts/openapi.yaml`. That document does not exist yet; current TypeScript constants, backend DTOs, and app models are separate incomplete definitions, not a complete canonical contract. Backend remains untouched before Phase 5.
- **Shared API/domain types (target):** `packages/shared-types` is the intended maintained TypeScript source for common types, but is not imported by production apps and does not currently eliminate Kotlin, Swift, or backend enum/model copies. API request/response models for clients must be generated from or mechanically checked against the OpenAPI contract. Platform-only persistence and OS types stay native.
- **Database schema:** `database/migrations/*.sql` is the PostgreSQL schema source. Do not maintain a second hand-written schema or duplicate SQL projections in controllers; that cleanup belongs to Phase 5.
- **Translations:** `packages/localization/{locale}.json` is the maintained source for the six supported locales. Android resource identifiers map through `packages/localization/native-key-map.json`; iOS uses canonical keys. Native resources are generated and checked by `scripts/localization/generate-native-locales.py` in CI. The original audit counts are retained in the status history as the pre-Phase-6 baseline.
- **AI validation schemas:** `packages/schemas` currently documents policy only; it is not a runtime schema package. Runtime schema validation belongs to the AI work in Phase 9.

These assignments record the target architecture; they do not claim the packages are currently consumed by the apps or CI. Their wiring is tracked in the phase status matrix.

## 3. Scope and priorities

### P0 — Dependable, usable alarm

- First launch without account; create, edit, enable/disable, delete, and test multiple alarms.
- Labels, weekday recurrence, stable chronological next-fire sorting, explicit timezone mode, and visible next occurrence.
- Native platform scheduling and audible delivery, including lock-screen/background behavior allowed by each OS; permission education and truthful schedule verification.
- Correct repair after app relaunch, process death, reboot, app update, time/timezone/DST change, alarm edit/delete, and permission change wherever the OS permits recovery.
- Sound selection and preview; vibration and gradual volume behavior only where supported; plain guidance about device volume and system limits.
- Wake session reached from the ringing alarm; reliable stop, snooze, retry, and emergency stop behavior.
- Local Math, Steps, and QR/barcode mission flows with instructions, validation, retries, timeout, and fallback. No login/network request in the active wake flow.
- Durable local alarms, wake sessions, and queue state; crash/restart recovery. The approved automated ten-point persistence gate is already recorded as passing; do not redo it without a regression. Device-level release checks remain open.
- Full six-language coverage and a first-alarm flow that tells the user what permissions are needed.

### P1 — Complete wake-up and progress toolkit

- Photo/object/place missions with local-first verification, confidence/retry guidance, and fallback.
- Exercise missions such as push-ups, squats, steps, and yoga/pose holds; camera/motion permission alternatives and false-negative recovery.
- Cognitive/text missions: memory, sequence, typing/rewrite, word matching, configurable affirmation or devotional text.
- Short mission sequences with effort estimate, per-step progress, bounded retries, and recovery.
- Optional wake-up check with user-controlled delay and transparent missed-check behavior.
- Dated history and accurate success/missed/technical outcomes, streaks, and progress derived from persisted sessions.
- Free/Pro boundaries and purchase restoration. A basic alarm and active wake flow remain available free and regardless of store/account availability.

### P2 — Optional morning and personalization services

- Localized weather and morning briefing, with purpose-specific optional location access, freshness labels, cache/offline handling, and no effect on alarms.
- Sleep sounds/timer and sleep insights only after privacy, battery, health-permission, and background behavior are specified.
- Optional social accountability, private by default and shared only by explicit action.
- Adaptive or AI recommendations with explanations and consent. They may suggest changes but cannot silently alter alarms, reject wake completion, or be required for any alarm.
- These items remain in product scope. They are sequenced after P0 and P1 foundations, not removed.

## 4. User journeys required for MVP

### A. First alarm

Install → choose language → understand the alarm and permission requirements → create an alarm → choose repeat/sound/mission → review time, timezone, next occurrence, and permissions → save → receive truthful scheduled status → run a test alarm.

### B. Wake

Operating-system alarm fires → audible alert and wake flow appear within platform limits → user sees the selected mission and controls → complete/retry/snooze/emergency-stop → persist outcome locally → show the result in history.

### C. Recovery

Permission denied/revoked, device capability unavailable, offline state, failed write, stale alarm, or app restart → explain the specific issue → offer a clear retry or safe fallback → do not mark the user as failed or claim an alarm is active when it is not.

### D. Offline repeat

After the alarm is configured, disconnect the network → alarm still fires → core selected mission still works → result persists → optional sync can retry later without duplicating events.

## 5. Ordered implementation plan and exit gates

Do not start a later phase until its dependencies are green. Each phase gets a narrow change set, CI run, commit, and status update. Physical-device gates are recorded separately from CI.

| Phase | Work | Exit gate |
|---|---|---|
| 0. Re-baseline and architecture contract | This document governs; retain existing user/device evidence; reconcile status; inventory confirmed gaps, duplicate sources, and production stubs without repeating already-green suites or the user's completed iPhone walkthrough. Define canonical API/schema/locale sources and phase ownership. | One ordered plan and one status matrix; no conflicting roadmaps; confirmed findings trace to code or existing CI evidence; no changes to backend before Phase 5. |
| 1. P0 launch and alarm reliability | Fix reported blank/partial launch paths and source-confirmed alarm-delivery gaps on both platforms: truthful storage errors, recurrence/recovery, time/zone/DST, permissions, sound failure fallback/vibration, independent Android simultaneous alarms, Android system alarm visibility/Direct Boot, and iOS notification-capacity policy. Reuse passing gates and add only tests for changed behavior. | App renders and responds on fresh install/relaunch; each enabled alarm has truthful readiness; targeted CI tests cover changed paths; no claim of audible physical delivery from simulator results. |
| 2. P0 wake-session integrity | Close test-alarm/statistics separation, snooze restore, duplicate triggers, AlarmKit stop-to-mission transition, durable recovery, and local event correctness. Do not rerun the already-confirmed emergency-stop scenario unless its code changes. | Targeted E2E verifies trigger, mission start, close/relaunch recovery, snooze, retry, completion and statistics; the alarm path works offline and has no fake success. |
| 3. P0 mission runtime | Finish Math, Steps, QR/barcode fallback and failure handling. Keep implementation local-first and separate test alarms from real wakes. | Targeted E2E covers offline completion, invalid QR fallback, timeout, permissions, duplicate trigger and test-alarm exclusion. Preserve already-confirmed iPhone walkthrough evidence; do not repeat it. |
| 4. Local data integrity | Preserve accepted ten-point persistence baseline; fix only proven gaps such as Core Data read failures appearing empty; verify migrations, queue, history, backup/data-loss behavior, and error recovery. | Targeted persistence tests pass; no data error is presented as an empty account; prior green ten-point suite is not repeated absent regression. |
| 5. Backend security and live sync | Before exposing a production API, fix anonymous account takeover/session invalidation, registration abuse controls, transaction-wide poison batches, cross-owner UUID handling, timestamp bounds, indexes, and repeated SQL/service logic. Define server-owned trial identity/history. Then connect real configured HTTPS client paths for identity, bidirectional alarm sync, wake sessions, diagnostics, and conflict recovery using the canonical contract. | Security and database tests pass against PostgreSQL; real app-originated records complete round trips; idempotency, ownership, partial failure, outage and recovery are covered; new anonymous registrations cannot mint/reset trial. Local alarms still work offline. No test endpoint or endpoint existence counts as product integration. |
| 6. UX and localization | Complete first-run and alarm-management flows, permission explanations, all user-facing strings, mission copy, notifications, settings, accessibility labels, date/time formats, and six locales. Wire Android onboarding; fix edit rotation, cancel/back and delete confirmation/error recovery. | Locale parity/fallback/plural/date/time tests and simulator/emulator walkthroughs pass; fresh-start/permissions and TalkBack/VoiceOver paths work; first alarm works without account. Preserve completed iPhone checks. |
| 7. Free/Pro, trial, and billing | Implement StoreKit and Play Billing, server entitlement verification, one-time introductory trial eligibility, restore/expiry handling, and localized terms. App registration never grants or resets a trial. | Reinstall, cleared app data, new anonymous registration, restore, expired purchase, and store outage cannot grant a second trial; basic alarm remains free and working. Store sandbox E2E passes on both platforms. |
| 8. P1 history and wake-up toolkit | Connect progress/history to real persisted wake sessions; then add photo/object, exercise, cognitive/text, sequences, and wake-up check in scoped slices, each with real local sensors/camera or provider behavior and deterministic fallback. | No fabricated series, scores, or events; local/API totals reconcile; each feature has offline/permission/error recovery, localization and focused CI coverage; no P0 regression. |
| 9. P2 weather and optional services | Add weather through a real configured provider/API, then sleep/social/adaptive/AI only after consent, privacy, freshness, battery and outage behavior are specified. | Real forecast includes source/time/freshness; no seeded weather or fabricated insight; denying location/network never affects alarms. |
| 10. Release validation | Verify signed store builds, migrations/upgrades, account/trial states, permissions, offline, lock-screen and audible behavior on physical iPhone and Android devices. Reuse the user's completed iPhone checks; request only a specific new check if changed code requires it. | All P0 gates pass on physical devices; no critical alarm/auth/trial defect remains; six locales, privacy/store assets, crash reporting, release signing, CI and documented OS limits are complete. |

### Current phase position

- Phase 0 — **complete on 2026-10-09**: the supplied plan, audit, repository, CI, and completed device evidence are consolidated in this plan and the sole status matrix. Confirmed duplicates, incomplete target sources, and false workspace/documentation claims are explicitly recorded and assigned to their implementation phases; native OS adapters are retained. Phase 0 closure verification is attached to the commit that records this status. This task does not start Phase 1.
- Phase 1 — **complete on 2026-10-09**. All recorded launch, local-readiness, recovery, recurrence, timezone/DST, permission, Android fallback/vibration, system-alarm/Direct Boot, and iOS notification-scheduling gates passed their targeted CI checks. Android independent simultaneous ringing is implemented in `20ca506`; its targeted identity test, Android build/unit/instrumentation, iOS jobs, persistence smoke, and repository jobs all passed in [CI run 37980721063](https://github.com/pronto1245/awero/actions/runs/37980721063). iOS ordinary-notification fallback truthfully warns that Silent Mode or Focus can limit sound; physical audible delivery remains a Phase 10 device check. The reported iPhone blank/partial-screen problem was fixed in `acc7fa6`; CI `37934795014` passed, and the user subsequently reported the app works on iPhone. Preserve that evidence; do not request or repeat the walkthrough.
- Phase 2 — **complete on 2026-10-10**. Test alarms are isolated from real history/statistics; snooze and mission/fallback state restore from persisted sessions; duplicate and concurrent deliveries are idempotent; iOS AlarmKit Stop enters the persisted mission flow; failed writes/scheduling remain retryable without false completion; Android offline wake/mission recovery and migration gates pass. The user-confirmed iPhone walkthrough and emergency-stop check remain accepted; neither was repeated. Targeted platform jobs passed in [CI run 37990744262](https://github.com/pronto1245/awero/actions/runs/37990744262). Its unrelated `repository-check` could not initialize PostgreSQL because Docker Hub rate-limited the unauthenticated image pull; backend files were not changed. See the Phase 2 closure evidence in the status matrix.
- Phase 3 — **complete on 2026-10-10**. Final Android and iOS platform gates passed in [CI run 37994679758](https://github.com/pronto1245/awero/actions/runs/37994679758); the user-confirmed iPhone walkthrough was preserved, not repeated. The unrelated repository-check Docker initialization failure happened before checkout. Physical Android and broader release validation remain Phase 10.
- Phase 4 — **complete on 2026-10-10**. Core Data reads preserve errors rather than returning empty values; both platforms show a retryable alarm-storage error; legacy migrations remain retryable and statistics migration markers prevent stale data from being reimported. Focused migration/read-failure tests and the associated full CI run passed in [CI run 37999302184](https://github.com/pronto1245/awero/actions/runs/37999302184). The previously accepted ten-point persistence gate was retained rather than separately repeated. The local data-loss boundary is documented; device-backup/reinstall validation remains Phase 10.
- Phase 5 — **implementation in progress; production acceptance awaits live configuration**. Security, API, Android, iOS and persistence jobs passed in [CI run 38026854386](https://github.com/pronto1245/awero/actions/runs/38026854386). This slice adds secret-based anonymous identity, per-operation sync rejection, owner-safe reconciliation, server bounds/indexes and request controls, service-owned API persistence, and iOS/Android send-and-pull paths. The production API URL is intentionally unset because the owner has not supplied a domain; app-originated round trips through a public HTTPS API are therefore unverified. Canonical OpenAPI/model parity remains outstanding. Anonymous registration issues no trial; verified purchase/trial history belongs to Phase 7. Do not mark this phase complete until the contract is canonical and live app-originated records complete a round trip through the configured HTTPS API.
- Phase 6 — **complete on 2026-10-10**: Android emulator and iOS simulator first-run tests verify accessible onboarding → alarm setup → saved alarm; Android waits for the Compose home state after save to avoid a UI assertion race and logs save errors for diagnosis. Six-locale parity, edit/cancel/system Back, deletion confirmation/recovery, localized permissions/errors, and large-text/accessibility semantics are covered. Commit `78d01a7` passed all four CI jobs in [run 38041916602](https://github.com/pronto1245/awero/actions/runs/38041916602). Physical VoiceOver/TalkBack operation remains a Phase 10 device check; the previously confirmed iPhone alarm walkthrough is preserved and was not repeated.
- Phase 7 — **not started**: store billing and trial enforcement. It must use store eligibility plus server-owned history; repeated anonymous registration can never reset eligibility.
- Phases 1, 2, 3, and 4 are complete after the accepted Phase 0 closure. Phase 5 still awaits a public HTTPS configuration and canonical OpenAPI/client parity; the owner explicitly directed work on the independent Phase 6 UX scope while that external configuration is pending. Preserve the user-confirmed iPhone evidence; do not repeat accepted checks without a related code change.
- Automated Local Persistence Hardening: recorded complete and green in CI.
- Physical iPhone and Android release matrix: remains a separate Phase 10 gate and is not implied by CI.
- Phase 6 closure evidence is recorded below after the added onboarding UI tests passed in CI; manual physical-device VoiceOver/TalkBack checks remain part of Phase 10.
- Core Math/Steps/QR flows and fallback exist; do not describe unavailable Photo/Mixed missions as implemented.
- Backend APIs and alarm outbound sync exist. Their presence does not mean end-to-end backend integration is product-complete; see the Phase 5 status above.
- Photo/exercise/weather/sleep/social/AI capabilities are roadmap scope, not shipped features.

## 6. Competitor findings → product requirements

The detailed source notes and review caveats are in [Competitor and Review Gap Analysis](research/COMPETITOR_AND_REVIEW_GAP_ANALYSIS.md).

| Competitor signal | AWERO requirement | Gate |
|---|---|---|
| Erly: mission-based wake-up, streak/history, photo/object and exercise tasks; visible complaints include timezone handling and mission/login flow. | Keep account independent from active wake; preview mission; show explicit timezone/next fire; provide deterministic retry/fallback. | Offline wake E2E, timezone/device tests, valid/invalid mission fixtures. |
| Alarmy: many mission types, wake-up check and sleep features; visible reports include missed alarms and ads interfering with dismissal. | Alarm delivery first; no ad/paywall/prompt can cover ringing, mission, snooze, or emergency stop. | Physical audible-alarm matrix and overlay exclusion E2E. |
| I Can't Wake Up!: configurable task chains and awake checks. | Allow bounded configurable mission sequence and optional post-dismissal check after P0. | Sequence completion/recovery tests and wake-check timing tests. |
| WAKR: push-up/yoga/photo/shake/breathing/memory, streak and weather forecast. | Include exercise missions in P1 and weather in P2; camera/motion errors never trap user; weather is optional. | Permission/false-negative fallback and weather-denied/offline/stale tests. |
| Wakey: mission alarm and optional morning weather/insight briefing. | Keep briefing outside alarm path; disclose paid boundaries before setup friction. | Offline alarm and entitlement-state tests. |

Research remains qualitative. Visible store reviews are not a representative sample, and competitor runtime testing is not complete. Expand review sampling before finalizing detailed P1/P2 choices; do not delay P0 alarm reliability for that research. No app can guarantee zero complaints; AWERO's goal is to prevent known failure classes with acceptance tests, then monitor verified alarm/wake failures and support reports after release without claiming a ring that the OS does not confirm.

## 7. Delivery rules

- One phase at a time. No unrelated design, feature, and infrastructure work in a reliability commit.
- Before implementation, state the target requirement IDs and expected acceptance tests.
- After each phase: run the relevant local tests, commit the scoped change, inspect CI, update implementation status, then proceed.
- A failed CI result must be fixed before starting the dependent phase.
- “Green CI” proves only the checks CI ran. It does not prove physical alarm delivery, localization quality, store approval, or installed competitor behavior.
- Keep an explicit decision/open-issue list for any OS behavior or product choice that cannot be inferred safely. Resolve it before the affected feature is implemented.
- Keep the current product list complete. Moving a feature to a later phase means deferred, not forgotten or removed.
- For every implementation slice, remove the related production stub/mock and connect the real local platform/storage API or live remote API before marking the slice complete. Use mocks only in tests.
- After each scoped implementation slice, run only its relevant checks, then commit and push that slice immediately and verify its CI result. Do not accumulate unrelated changes or create temporary duplicate project copies.
- Preserve user-owned dirty files and unrelated commits. Inspect the exact diff and stage only the current slice; never reset, stash, cherry-pick wholesale, or push unrelated local work without explicit instruction.
