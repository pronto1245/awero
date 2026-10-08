# AWERO Product Specification and Implementation Plan

**Status:** Working baseline for implementation; product code is not declared production-ready.  
**Platforms:** iOS and Android.  
**Baseline date:** 2026-10-08.  
**Owner:** AWERO product owner.  
**Research:** [Competitor and Review Gap Analysis](research/COMPETITOR_AND_REVIEW_GAP_ANALYSIS.md).  
**Feature inventory:** [Product Requirements](PRODUCT_REQUIREMENTS.md).  
**Implementation status:** [Implementation Status](IMPLEMENTATION_STATUS.md).  
**Persistence gate:** [Local Persistence Hardening](LOCAL_PERSISTENCE_HARDENING.md).

This is the governing product and delivery plan. If an older roadmap or status summary gives a different phase order, use this document and reconcile the older file before starting work from it. A capability is not complete because a model, API, screen mock, or CI job exists: it must satisfy its acceptance criteria on both platforms.

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
| 0. Product baseline | This document governs; reconcile links/order in older plans; maintain a capability/status matrix; collect competitor review examples and mark research limits. | No conflicting phase orders; every in-scope feature has priority, owner/platform coverage, acceptance evidence, and honest status. |
| 1. Alarm reliability — code and CI | Audit current iOS and Android scheduling paths against actual OS APIs and store policy; fix permission, reschedule, edit/delete, reboot, time/timezone/DST, sound, lock-screen, and failure-recovery gaps. | CI tests for scheduling, cancellation, recurrence, permissions, stale schedules, and recovery are green on both platforms; state is reported truthfully. Physical iPhone/Android verification is deferred to Phase 10 and is not claimed as passed. |
| 2. Wake-session runtime | Connect actual ring to wake UI; implement stop, snooze, emergency stop, retry/fallback, duplicate-trigger handling, and local event persistence. | Device E2E from ring through each terminal path; no sign-in, paywall, ad, prompt, or network dependency interrupts the flow. |
| 3. P0 mission runtime | Finish user-facing Math, Steps, QR/barcode missions and settings; deterministic local validation; clear retry, timeout, permission-denied, and fallback behavior. | iOS and Android tests plus device checks; offline mission E2E; test alarm excluded from history. |
| 4. Local persistence | Preserve the approved ten-point hardening baseline. Fix only demonstrated regressions/gaps; verify alarm, session, history, and queue restoration on devices. | CI persistence suite green; device relaunch/reboot and duplicate/retry cases recorded. Existing automated gate is complete; physical checks are not implied by CI. |
| 5. Backend and sync | After local reliability, mission, and persistence gates: audit existing API code against requirements; implement or repair anonymous identity, device registration, alarm/session sync, conflict rules, idempotency, retry, and diagnostics. | Local alarm/wake E2E still passes with backend offline; sync is owner-scoped and idempotent; outage/retry tests pass. Reuse verified APIs and do not rebuild them. |
| 6. UX and localization | Complete first-run and alarm-management flows, permission explanations, all user-facing strings, mission copy, notifications, settings, accessibility labels, date/time formats, and six locales. Keep visual redesign deferred until behavior/content are accepted. | Key parity, locale fallback, plural/date/time formatting tests; native-language walkthrough on both platforms; first alarm works without account. |
| 7. Free/Pro and billing | Define exact entitlements; implement StoreKit and Play Billing, restore/expiry handling, and localized terms. | Basic alarm/wake flow remains available in every purchase, restore, offline, and store-error state; purchase E2E on both test tracks. |
| 8. P1 missions and wake-up toolkit | Add photo/object, exercise, cognitive/text, sequences, wake-up check, and trustworthy progress/history in small feature slices. | Each feature has offline/permission/error/fallback tests, six-language coverage, device validation, and no P0 regression. |
| 9. P2 optional services | Add weather/briefing, then sleep/social/adaptive/AI only after consent, privacy, freshness, battery, and outage behavior are specified. | Denial/offline/outage cannot affect alarms; privacy and store requirements pass review. |
| 10. Release validation | Freeze scope; run install → schedule → lock → audible alarm → mission → recovery → history → offline flow on current iPhone and Android test devices; inspect upgrade/migration and store builds. | All P0 gates pass on physical devices; no open critical alarm/wake defects; six locales complete; CI green; release notes and OS limits accurate. |

### Current phase position

- Phase 0 — product scope, competitor inputs, phase order, and acceptance gates are reconciled across the governing specification and linked plans. **Complete.**
- Next: Phase 1 — verify and complete audible alarm reliability on physical iPhone and Android devices.
- Automated Local Persistence Hardening: recorded complete and green in CI.
- Physical iPhone alarm delivery: **not passed**; the reported push notification without audible ringing is not an alarm-delivery pass. Physical iPhone and Android checks are deferred to Phase 10.
- Android physical alarm delivery: **not recorded as passed**. Code-level reliability work and CI proceed now.
- Six-language resource foundation exists; full app wiring and validation are **not complete**.
- Core mission logic/foundations exist, but user-facing mission flows, retry/fallback UX, and end-to-end delivery are **not complete**.
- Backend foundations exist in the repository. Their presence does not mean backend integration is product-complete.
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
