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

## Governing plan position
- [x] Phase 0 — combine the cross-platform architecture audit and user-supplied audit into one ordered plan; preserve existing green CI and completed iPhone walkthrough evidence
- [ ] Phase 1 — **current**: P0 launch and alarm reliability. Reported iPhone blank/partial launch and nonresponsive behavior is an open blocker. iOS storage-read errors now have a distinct loading/error/retry state (commit `bb0f04b`); Android audio failure falls back to an alarm tone and vibration (commit `8dbc032`). Both changes passed the full CI run at [37946349952](https://github.com/pronto1245/awero/actions/runs/37946349952). Android publishes enabled alarms through `setAlarmClock` in `aa430b0`, passed all jobs in [37948061694](https://github.com/pronto1245/awero/actions/runs/37948061694). Direct Boot schedule restoration is in `b5ff749` with the compile correction in `e5f4912`; all jobs passed in [37951703595](https://github.com/pronto1245/awero/actions/runs/37951703595). iOS notification fallback now verifies every selected weekday remains pending and rolls back a partial schedule; focused XCTest passed in [37953899030](https://github.com/pronto1245/awero/actions/runs/37953899030). iOS fallback sound limitations and full alarm setup remain open. Accept the user's existing iPhone walkthrough without repeating it.
- [ ] Phase 2 — baseline automated wake-session gate passed, but iOS snooze restore and test-alarm/statistics gaps remain; Android currently permits only one active wake session, so simultaneous alarm lifecycle must be addressed end-to-end here
- [x] Phase 3 — automated Math, Steps, and QR/barcode gate passed; the iPhone walkthrough is user-reported complete and must not be repeated
- [x] Phase 4 — ten-point automated local-persistence gate passed; targeted iOS read-error presentation gap remains
- [ ] Phase 5 — **partial and security-blocked for production**: audit confirms anonymous registration by known `deviceId` can take over an account and revoke its sessions; a bad sync operation rolls back/retries the whole batch; a foreign alarm UUID can hit a primary-key 500; request throttling/security headers are absent; future wake-session timestamps are accepted; owner indexes are missing. Full mobile alarm download, wake-session sync, diagnostics submission, and user-facing conflict resolution remain incomplete.
- [ ] Phase 6 — **partial**: UX/onboarding acceptance remains open. Android onboarding is not connected to startup; Android edit state uses non-saveable state; delete error/confirmation and active-path localization/accessibility gaps remain. The user-confirmed iPhone walkthrough stays accepted and must not be repeated.
- [ ] Phase 7 — **not started**: billing and trial eligibility. No current trial ledger exists. New anonymous registrations must never mint/reset a trial; store eligibility and server-side trial history must agree.

## Cross-phase acceptance rule

Every implementation slice uses real production data paths: native OS APIs and local persistence for offline alarm/wake behavior, and a live configured HTTPS API for remotely backed features. Production screens must not contain mock responses, seeded demo records, fabricated statistics, or fake success states. Mocks/fixtures are allowed only in test targets. Empty, offline, permission-denied, and service-error states must be truthful. This rule applies immediately in every phase. Do not enable a production API until its auth and abuse controls pass Phase 5; a missing URL must remain an explicit unavailable state, not simulated data.

Both mobile sync configurations currently return “not configured” when no HTTPS base URL is supplied. The repository scan found test fixtures but no obvious production code tagged `mock`, `stub`, `fake`, `demo`, or `placeholder`; that text scan does not prove every runtime path is real. Inspect behavior in each feature slice and keep remote functionality explicitly unavailable until its live service is configured. The user has not yet supplied a public API domain.

## Consolidated audit findings and ownership

The supplied audit is dated 2026-10-09 and names commit `785093a`; the current repository baseline is `64b5061`. The inspected API/security and alarm files relevant to its critical claims are unchanged between those commits. Findings below are assigned to the governing plan rather than repeated as another independent plan.

- **Phase 1 — alarm and launch:** Android sound failure fallback/vibration is implemented in `8dbc032` and passed CI run `37946349952`; the service still has one player and the notification uses fixed ID `7001`. Simultaneous wake behavior is assigned to Phase 2 because the wake-session store enforces one active session. `aa430b0` publishes schedules via `setAlarmClock` and passed all jobs in `37948061694`. Direct Boot stores only schedule fields in device-protected storage and restores them on `LOCKED_BOOT_COMPLETED`; `b5ff749` plus `e5f4912` passed all CI jobs in `37951703595`. On iOS notification fallback, the scheduler verifies all selected weekdays remain pending, preserves already-scheduled alarms if iOS silently drops a new request, and returns a localized error for an incomplete schedule; focused XCTest passed in `37953899030`. This guard does not assume an undocumented fixed capacity. iOS pre-26/fixed-timezone fallback still uses ordinary `.default` notifications. iPhone blank/partial-screen and freeze reports are user-observed, not dismissed by simulator CI.
- **Phase 2/4 — wake and local data:** iOS test notifications enter the wake flow; targeted code paths must keep tests out of progress. Android wake persistence currently permits one active session, so simultaneous alarms require per-session storage and actions. Snooze restore remains open. Core Data fetch errors now surface as a distinct error/retry state in `bb0f04b`, validated by CI run `37946349952`. Never repeat emergency-stop or full manual iPhone walkthrough tests unless that exact behavior changes.
- **Phase 3/6 — missions and UX:** `OnboardingScreen` exists but is not in the startup route. Android edit state is held in `remember`, so configuration recreation can discard the alarm being edited; deletion lacks confirmation/error recovery. Active wake/setup paths still contain hard-coded text and accessibility gaps. Localized resources exist in six locales; complete their use and parity, not merely file presence.
- **Phase 5 — backend security/integration:** `POST /auth/anonymous` accepts caller-supplied `deviceId` as identity and expires all prior sessions for that owner. `/sync` applies a batch in one transaction and throws on a bad operation; clients retry failed batches. A foreign alarm UUID may reach an insert and violate the primary key. `main.ts` does not install rate limiting, Helmet, or CORS configuration; CORS absence is not itself an exploit for native clients. The audit's 200/200 registration figure is reported empirical evidence and was not reproduced during this read-only review. No date-range validation protects `scheduledAt`/`triggeredAt`; migrations lack indexes for the anonymous-owner lookup fields. API endpoint presence is not client integration: wake-session, analytics, and support-diagnostics sender paths are missing from the apps; alarm download/reconciliation and user-facing conflict recovery are incomplete.
- **Phase 5/7 — identity and trial:** no billing/trial implementation or trial-history table is present. A new `deviceId` can create another anonymous user; app registration must never issue trial access. Store eligibility is authoritative for store introductory offers; server-owned state is authoritative for any AWERO-specific trial.
- **Architecture cleanup — every phase:** CRUD and sync repeat alarm SQL projections and field mapping; controller code owns persistence logic; model/enums/validation are maintained separately across backend, Swift, and Kotlin while `shared-types`/`api-contracts` are not wired into the clients. Locale JSON exists, but native UI contains hard-coded runtime strings. Treat duplicate business rules as drift risk; preserve native OS adapters. README also lists `apps/worker`, `apps/admin`, and `packages/validation`, which are absent. No `pnpm-lock.yaml`, Gradle wrapper, or `PrivacyInfo.xcprivacy` was found; API scripts name Jest/ESLint without declaring them, while CI runs build/typecheck/smoke rather than backend unit/lint suites. These release/infrastructure items belong to the assigned phases, not a one-off repository rewrite.

The estimate of 27–38 developer days in the supplied report is a planning estimate, not a source-verifiable fact. Platform-specific claims such as the exact iOS pending-notification cap and AlarmKit recovery after closing the app need narrowly scoped platform confirmation; they are not grounds to repeat the completed iPhone mission walkthrough.

## Previously accepted evidence and boundaries

- Green CI proves only the jobs listed in that run; it does not close missing client/API flows or device gates.
- The governed phase table is in [AWERO Product Specification and Implementation Plan](AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md). Historical CI notes below document what was tested; they do not override the current phase gates.
- The user-confirmed iPhone walkthrough/manual checks are accepted as completed. The source audit identified code-level gaps but does not authorize repeating the same manual checks.
- Remaining product scope includes billing, P1 progress/history and missions, P2 weather/sleep/social/AI, and the broader Phase 10 release matrix.
- Never reset or stage unrelated dirty files/commits. Each scoped change is committed and pushed immediately; verify the resulting CI once and proceed without rerunning unchanged green suites.

## Phase 2 automated gate — complete

- Baseline `34932a904bae95069da485edf9cef45ac43fa5d7` passed all four jobs in [AWERO CI run 574](https://github.com/pronto1245/awero/actions/runs/37832150008).
- Commit `da3992ad810ce152a8445c0712a8dd049e4191f7` prevents overlapping iOS snooze requests from cancelling the successfully scheduled snooze. Two targeted local XCTest cases passed, followed by all four jobs in [AWERO CI run 575](https://github.com/pronto1245/awero/actions/runs/37833128379).
- Commit `28c1ae960d9515869b6c91101b8b6dc7dd93ffb5` makes Android wake UI render the restored controller state and selected mission, including a persisted Math fallback. Its emulator E2E re-delivers an alarm intent, checks the same session resumes, solves the fallback and verifies persisted success. All four jobs passed in [AWERO CI run 576](https://github.com/pronto1245/awero/actions/runs/37834366415).
- Commit `ee3e2df07473c42e6d583321b93ab2388453178e` makes iOS mission start, fallback and completion report persistence failures and offer a retry of the failed action. A failed write keeps the session active and does not advance mission/result state. Three targeted local XCTest cases using a read-only store passed, followed by all four jobs in [AWERO CI run 577](https://github.com/pronto1245/awero/actions/runs/37835695292).
- Android now also rejects overlapping snooze requests while the Room write is pending, preserving the successful OS schedule. A new emulator test sends the second tap during persistence, verifies one schedule/no cancellation and the saved count, then verifies another snooze after the next delivery. Commit `a325fab24ec97c6f5059d2ae547e6bcfd1949deb` passed all four jobs in [AWERO CI run 578](https://github.com/pronto1245/awero/actions/runs/37837964796).
- Backend/sync changes and physical-device release gates are outside this correction.

## Phase 3 — automated gate complete

The first package addresses P0 Mission runtime / recovery requirements: two-minute Steps/QR timeout to local Math, manual fallback, camera/sensor cleanup when the mission view is detached, and retrying a failed save after a validated mission. Math remains the deterministic local fallback.

The next scoped package adds camera-based QR/barcode capture during alarm creation/editing on both platforms, an optional exact-content field, blank-code save prevention, persisted expected code and iOS Math difficulty selection. Android scans ML Kit-supported formats; iOS uses the supported subset of QR, EAN/UPC-E, Code 39/93/128, PDF417, Aztec and Data Matrix. Permission denial/camera failure during setup preserves the draft and offers manual entry. The iOS persistence/edited-code validation test passed locally; Android's existing restart test now includes the expected code.

Android instrumentation verifies test alarms stay out of wake statistics, the alarm and mission flow works with emulator networking disabled, a validated result can be retried after a failed write, and denied camera permission persists a Math fallback that can complete successfully. These gates passed in [CI run 37844092499](https://github.com/pronto1245/awero/actions/runs/37844092499), [CI run 37877786906](https://github.com/pronto1245/awero/actions/runs/37877786906), and [CI run 37881243655](https://github.com/pronto1245/awero/actions/runs/37881243655), all four jobs green. The user also confirmed the guided QR, invalid-code fallback, Math, Steps and snooze walkthrough works on iPhone. Physical Android coverage and the broader release matrix remain Phase 10 checks. Full localization stays in Phase 6.

### User-reported iPhone walkthrough

On 2026-10-08, the user confirmed that the guided iPhone walkthrough worked. The walkthrough covered creating a QR alarm, scanning the saved code at wake-up, invalid-code fallback to Math, Steps, and snooze. This is a manual report without recorded device/OS details; it does not replace the broader Phase 10 release matrix.

## Phase 5 — historical CI/API work (product gate remains partial)

The first read-only audit of anonymous auth, alarm ownership/versioning, wake-session lifecycle, offline sync, analytics and support idempotency found that wake-session retries could reuse an ID with a changed mission/timestamp and wake-event retries did not compare a supplied timestamp. The API now rejects those changed-content retries, and the repository smoke scenario asserts both conflicts; all four jobs passed in [CI run 37882395259](https://github.com/pronto1245/awero/actions/runs/37882395259). A follow-up audit found that identical retries with omitted optional `occurredAt` fields could conflict because the server generated a new time each attempt. Sync, analytics and wake-event retries now ignore generated time when the client omitted it; smoke coverage confirms the retry result remains stable. Commit `f939239db0e49d30b588cb10b37e60cd3d902a6f` adds automatic retry when the app returns to the foreground. [CI run 603](https://github.com/pronto1245/awero/actions/runs/37887632818) passed all four jobs. The Android/iOS client tests cover transport errors, persistent queue backoff and retry after restart; the API smoke suite covers idempotent sync retries and conflicts. These are valid CI/API results, but they do not prove a complete client round trip for alarms, wake sessions, diagnostics, or conflict resolution. Phase 5 remains product-partial; Phase 6 UX work is partial and follows Phases 1–5; Phase 7 remains blocked on the Phase 5 acceptance gaps.


## Phase 6 — historical UX implementation record

The first UX/localization slice updates the iOS and Android home screens to the approved warm ivory, navy and coral design. It selects the next enabled alarm using each alarm’s configured timezone, shows repeat days and mission, supports enable/disable, and explains that the first alarm works offline without an account. Home-screen copy and actions now have matching keys in English, Russian, Brazilian Portuguese, French, German and Spanish; the locale parity check covers those keys. Alarm setup, permission education, mission and notification copy are implemented. Home and mission text now respect system time formats and Dynamic Type; create/edit screens expose device-following or fixed time-zone behavior. Settings is reachable from the approved gear control on both platforms, shows the current device language, and opens the phone’s app-permission settings.


The second Phase 6 slice completes the alarm setup flow on both platforms: repeat-day selection, localized mission/difficulty controls, QR setup guidance, and permission explanations. iOS now shows its notification-permission explanation before the first system request instead of requesting access at app launch. Android persists selected repeat days and offers Settings when required access blocks saving. Setup copy has exact six-locale key parity.


The localized alarm-home and alarm-setup slices passed all four jobs in [CI run 37912583144](https://github.com/pronto1245/awero/actions/runs/37912583144). A Phase 6 slice translated the wake controls and Math, Steps and QR mission prompts across both apps. Its acceptance is tracked by the six-locale parity check and the platform CI.


The home, setup and wake/mission packages passed all four CI jobs in [run 37915377895](https://github.com/pronto1245/awero/actions/runs/37915377895). This Phase 6 package wires localized notification contents and schedule/permission errors into both platform runtimes.


The Phase 6 accessibility/localization slice gives each iOS alarm switch a localized VoiceOver label that includes its time and uses the existing localized stopped-state title. Commits [a00aac7](https://github.com/pronto1245/awero/commit/a00aac7726cbf0a3cf7102ce7a9ec4fa1d654227) and [49c1d35](https://github.com/pronto1245/awero/commit/49c1d357edc7f4e55457b007c5e3e132c01f73d4) passed all four jobs in [CI run 37920473384](https://github.com/pronto1245/awero/actions/runs/37920473384). Remaining Phase 6 UX gaps are recorded in [Product UX Design](design/PRODUCT_UX_DESIGN.md); Phase 6 follows the current P0 work.

The latest accessibility/localization packet adds the approved gear entry and a localized Settings screen on iOS and Android. The screen identifies the device language and links to app permissions; its labels and settings copy have exact parity in all six locales. The QR camera preview now has a localized screen-reader description, and the primary wake and mission text scales with Dynamic Type. Commit [2f08649](https://github.com/pronto1245/awero/commit/2f08649b57232138dce73825177a43eb3bfe2c43) passed all four jobs in [CI run 37928994590](https://github.com/pronto1245/awero/actions/runs/37928994590).


The user reports the iPhone walkthrough and manual checks are complete; do not ask for or repeat that work. The code audit still identifies Android first-run routing, hard-coded strings in the active alarm-delivery path, and accessibility/localization implementation gaps. Close those code gaps and reconcile existing manual-check evidence before accepting Phase 6. The full physical alarm reliability matrix remains in Phase 10; the already confirmed iPhone walkthrough is not to be repeated.
