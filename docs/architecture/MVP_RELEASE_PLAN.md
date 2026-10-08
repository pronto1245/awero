# AWERO MVP Release Plan

## Goal

The MVP is complete only when a new user can:

1. Install AWERO.
2. Start without an account.
3. Create one alarm.
4. Run a test alarm.
5. Lock the phone.
6. Receive the alarm.
7. Start a mission.
8. Complete or recover through fallback.
9. See the wake session in statistics.
10. Repeat the flow offline.

## Critical implementation order

1. Native alarm reliability.
2. Wake session lifecycle.
3. Mission runtime and validation.
4. Fallback.
5. Snooze and emergency stop.
6. Local persistence.
7. Onboarding and permissions.
8. Localization.
9. Backend sync.
10. Subscription.
11. Adaptive policy.
12. AI gateway.
13. Automated QA.
14. Physical-device release validation.

Competitor and review findings refine acceptance criteria without changing this implementation order: [Competitor and Review Gap Analysis](../research/COMPETITOR_AND_REVIEW_GAP_ANALYSIS.md). AWERO is not considered complete from a feature checklist alone: both platforms must pass the alarm and wake-flow failure matrix below.

## Non-negotiable invariants

- Backend outage never prevents a scheduled local alarm.
- AI outage never prevents a scheduled local alarm.
- Internet outage never prevents Math, Steps or QR missions when their device capabilities are available.
- Subscription expiry never disables the basic alarm.
- AI cannot change alarm time silently.
- AI cannot delete or disable alarms.
- Every mission has a recovery path.
- Technical failure is not counted as a user failure.
- Emergency Stop is always accessible.
- Local alarm state is the source of truth for critical scheduling.
- No ad, review prompt, account gate, paywall, or network-dependent screen can interrupt ringing or mission completion.
- Alarm time, repeat days, timezone behavior, and the next scheduled occurrence are clear; edits never silently change the user's chosen time.
- A mission validation or device capability failure must provide retry or fallback and must not trap the user.

## Final release gate

A release candidate is accepted only after both platforms pass:

Install → onboarding → create → test → sleep/lock → alarm → mission → success → statistics

and the failure matrix:

offline, backend unavailable, AI unavailable, denied permissions, reboot, timezone change, DST boundary, app update, subscription expiry, camera unavailable, motion unavailable.

Physical-device execution is mandatory for the final release gate.
