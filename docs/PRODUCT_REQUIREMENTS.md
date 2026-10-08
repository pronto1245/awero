# AWERO Product Requirements — competitor-informed

**Status:** requirements and release criteria; not a claim that listed functionality is already implemented.  
**Platforms:** iOS and Android.  
**Research baseline:** 2026-10-08.  
**Related docs:** [Competitor and Review Gap Analysis](research/COMPETITOR_AND_REVIEW_GAP_ANALYSIS.md), [MVP Release Plan](architecture/MVP_RELEASE_PLAN.md), [System Implementation Order](architecture/SYSTEM_ORDER.md).

## Product outcome

AWERO must be a dependable alarm and wake-up system first. It should offer the useful mission, routine, weather, and progress features users expect from Erly, Alarmy, I Can't Wake Up!, WAKR and similar products, while removing known failure modes from the actual wake path.

“Every feature” means the complete competitor-informed scope below is accounted for in the product and its release stages. A feature is not complete because it appears in a screen: it needs platform implementation, localization, offline/recovery behavior where relevant, and acceptance evidence.

## Non-negotiable product rules

1. A basic alarm works without account, subscription, backend, AI or internet.
2. The alarm is scheduled by the operating system on both platforms. AWERO explains and verifies required permissions, and never reports success if scheduling failed.
3. Nothing online or commercial can block ringing, mission completion, snooze, emergency stop or fallback.
4. A permission, model, camera, motion sensor, storage or OS failure must lead to a clear retry or recovery path. It is not recorded as the user's failure.
5. Alarm times, repeat days, timezone behavior, sound and mission are shown before save. Edits are explicit and never silently change the user's chosen schedule.
6. Every user-facing string ships in English, Russian, Portuguese (Brazil), French, German and Spanish: onboarding, alarm setup, missions, errors, notifications, settings, support and Free/Pro terms.
7. UI must not claim a wake, streak, weather freshness, mission success or alarm delivery that local data cannot substantiate.
8. App Store / Play policy and OS limits are surfaced plainly. Do not promise that an app can prevent force-stop, power-off, silence mode, or other system actions where the platform does not permit it.

## Complete capability inventory

Priority indicates implementation/release sequence, not feature deletion.

| Capability | AWERO requirement | Stage |
| --- | --- | --- |
| Alarm creation and management | Multiple alarms; label; enable/disable; edit/delete; weekday recurrence; next occurrence sorted chronologically; test before relying on it; explicit time/timezone summary. | P0 |
| Real alarm delivery | Native scheduling, audible alarm behavior, lock-screen entry to wake flow, permission checks, versioned edits, reboot/time/timezone/DST repair, stale delivery prevention. Platform-specific implementation and limits documented separately for iOS and Android. | P0 |
| Sound and controls | Select available alarm tone, vibration where supported, sound preview, sensible level guidance, optional gradual ramp where supported, visible snooze policy and always-available emergency stop. Never claim control over the device's master volume where the OS does not grant it. | P0 |
| Mission runtime | Math, Steps and QR/barcode run locally when capability is available. Clear rules, validation, retry, timeout and fallback. A test alarm does not count as a wake. | P0 |
| Offline / recovery | Alarm rings and selected MVP missions work offline; session resumes after process death; failed sync queues locally; no login or network request takes over the active alarm. | P0 |
| Localization | Six required locales cover all screens, prompts, errors, permissions education and support flows on both platforms. | P0 |
| Photo / object / place proof | Camera task with user-selected target or item-search challenge. Prefer local/on-device verification. If confidence is low, ask for another capture or offer fallback; never trap the user. | P1 |
| Exercise missions | Push-ups, squats, steps and yoga/hold-a-pose challenges with accessible alternatives. Sensor/camera validation explains how to retry and falls back on permission denial or unreliable detection. | P1 |
| Cognitive and text missions | Memory, sequence, typing/rewrite, word matching, affirmations or devotional text; allow user configuration and a quiet preview/test before bedtime. Keep content localized and validators deterministic. | P1 |
| Mission sequences | Let users choose one mission or a short sequence, with estimated effort, per-step progress, retry policy and a bounded recovery path. | P1 |
| Wake-up check | Optional second check after dismissing the first mission to reduce falling back asleep. User controls delay; missed check follows transparent re-ring/escalation behavior. | P1 |
| Progress and accountability | Dated wake history, completed/missed/technical outcomes, best/current streak, and success rate only from persisted sessions. Editing timezone or test alarms must not falsify history. | P1 |
| Weather and morning briefing | Optional current conditions and concise forecast with readable visual treatment; may include a short morning summary. Location access is optional, purpose-specific and explained. Network/location failure yields cached-and-labeled or omitted weather; it never affects alarm setup or delivery. | P2 |
| Sleep support | Optional sleep sounds/timer and sleep insights only after privacy, battery, health-permission and background limits are specified. Not required to use the basic alarm. | P2 |
| Adaptive / AI assistance | Recommendations can suggest routine or mission changes with reasons and consent. They cannot change, disable or move alarms, reject a wake without recovery, or become a dependency of alarm delivery. | P2 |
| Social accountability | Optional sharing/accountability only with consent; private local use stays complete. Never reveal wake history without explicit user action. | P2 |
| Free / Pro | Basic alarm and reliable wake flow stay usable free. Feature boundaries and renewal are visible before setup friction; restore/expiry/store outage cannot break scheduled alarms. | P1 |

## Competitor signals mapped to AWERO

| Observed signal | AWERO response | Required proof |
| --- | --- | --- |
| Erly and Alarmy reviews report missed or late alarms; Erly reports manual timezone edits | P0 reliability gate on both platforms; show next fire time; recover after reboot/time/timezone/DST and compare scheduled versus observed time. | Physical iPhone and Android matrix, including locked/offline/reboot/time-change/permission-revoked/edit/delete cases. |
| Erly users report mission validation rejecting a valid object or affirmation | Start MVP with deterministic local missions; for camera/AI missions expose confidence recovery, retry and alternate task. | Valid/invalid fixtures plus low-confidence, poor light, camera denied, offline, timeout and fallback tests. |
| Erly review reports submitting math routed to login; Alarmy reviews report ads/lag interfering with dismissing | Active wake flow is local and isolated from sign-in, ads, paywall, review prompt, sync and network. | Run alarm session offline and with expired entitlement; dismiss via mission, retry, snooze and emergency stop. |
| Users report confusing edits, sorting, changed settings and sound surprises | Sort by next occurrence; preview sound/mission; show repeat/timezone/next fire; save/cancel visibly; test migration and edit paths. | UI and persistence tests for create/edit/cancel/delete/repeat/timezone and sound preferences; no stale alarm delivery. |
| Users value streaks but distrust inaccurate records | Persist dated session events before showing streaks; separate technical failure and test run. | Duplicate, crash/restart and offline history tests; hand-check streak boundaries and timezone/day rollover. |
| WAKR / Wakey show weather with exercise or morning routine features | Include optional, localized weather briefing and exercise-mission work in product scope after P0. Weather cannot request location as a prerequisite to setting an alarm. | Weather off/denied/offline/stale-location tests; exercise false-negative/permission/fallback tests. |

## Release gates

A capability is complete only when all apply:

- Implemented on iOS and Android, or a documented platform-specific alternative provides the same user outcome.
- All six locales have matching keys and correct fallback behavior.
- No account/network dependency exists in the alarm-critical path.
- Success, failure, denial, retry, timeout, offline, process-death and recovery cases are tested as applicable.
- The physical-device release matrix passes for P0 alarm and wake flow.
- Support diagnostics can explain a failure without collecting unnecessary personal data.

P0 must pass on physical iPhone and Android before the MVP is called ready. P1 is the next complete feature layer. P2 features remain in the roadmap and cannot weaken P0 reliability.

## Research sources

- [Erly App Store listing supplied by the product owner](https://apps.apple.com/ge/app/erly-wake-up-early/id6751428380?l=ru)
- [Erly Google Play listing and visible reviews](https://play.google.com/store/apps/details?id=com.erly.myapp)
- [Alarmy Google Play listing and visible reviews](https://play.google.com/store/apps/details?id=droom.sleepIfUCan)
- [I Can't Wake Up! Google Play listing](https://play.google.com/store/apps/details?id=com.kog.alarmclock)
- [WAKR App Store listing](https://apps.apple.com/pl/app/wakr-push-up-alarm-clock/id6768554854)
- [Wakey alarm listing with morning briefing](https://apps.apple.com/pl/app/alarm-clock-missions-wakey/id6759577760?platform=vision)
