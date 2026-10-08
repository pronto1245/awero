# Competitor and Review Gap Analysis

**Research date:** 2026-10-08  
**Purpose:** Turn public competitor screens, store descriptions, and user reviews into testable AWERO requirements. This is product research, not a claim that AWERO has implemented these features.

## Method and limits

Reviewed the App Store listing supplied for [Erly: Wake Up Early](https://apps.apple.com/ge/app/erly-wake-up-early/id6751428380?l=ru), its publicly shown screenshots and available review excerpts; the official Google Play listings for [Erly](https://play.google.com/store/apps/details?id=com.erly.myapp), [Alarmy](https://play.google.com/store/apps/details?id=droom.sleepIfUCan), and [I Can't Wake Up!](https://play.google.com/store/apps/details?id=com.kog.alarmclock); plus public App Store listing/review excerpts for Erly and Alarmy. We examined storefront material and screenshots, not a signed-in, installed runtime. Store ratings and review counts vary by country and change over time. Review examples are qualitative signals, not a representative or statistically sampled survey.

## What the products show

| Product | Publicly described strengths | Product gaps or risks to learn from |
| --- | --- | --- |
| **Erly** (iOS and Android listings) | Mission-based dismissal; push-ups, item search/photo, scripture/devotional, sky photo and make-bed missions in public screenshots; daily accountability; streak and wake history; goal deadline locks four hours before wake time. Subscription is required for full access. | App Store listing currently declares English only. Reviews mention alarms sorted by recently edited rather than time, object/affirmation recognition falsely rejecting valid responses, alarm/time edits becoming difficult after a change, timezone handling, and paywall expectations. |
| **Alarmy** (iOS and Android) | Broad alarm controls and sounds; math, memory, typing, movement, photo, QR/barcode and other dismissal missions; wake-up check; sleep-related features. | Reviews mention missed alarms, ads interrupting the dismissal / awake check, paywall confusion, sound level surprises, and mission validation errors. Any such report needs careful repro; it is not proof of general failure. |
| **I Can't Wake Up!** (Android) | Up to eight configurable tasks, including math, memory, sequence, barcode, rewrite, shake and word matching; task testing; volume ramp; awake test; configurable snoozes; anti-quit options. | Public reviews include complaints about confusing settings after redesign, removed/changed options, and playlist playback problems. The listing is dated, so verify current runtime behavior before treating every described feature as current. |

## Review signals and AWERO requirements

| Signal from public user reviews | Requirement for AWERO | Acceptance evidence |
| --- | --- | --- |
| Alarm fails to sound, sounds late, or is missed after timezone changes | Treat iOS and Android alarm delivery as the first release gate. Show exact-alarm, notification, full-screen and platform limitations before claiming an alarm is armed. Recover after reboot, time/timezone changes and app updates. | Physical-device matrix: locked screen, app backgrounded/terminated as supported by OS, offline, reboot, timezone/DST change, permission denied/revoked, alarm edit/delete. Record scheduled and observed trigger time. |
| A legitimate mission result is rejected (photo/object, spoken affirmation, math) | Use deterministic local validation for MVP missions wherever possible. Provide clear retry guidance and a bounded fallback; technical/model errors cannot trap the user or count as failure. | Valid/invalid mission fixtures, permission denial, camera/motion unavailable, timeout and fallback tests on both platforms. No network dependency in the wake path. |
| Alarm list is hard to scan; edits or time changes are confusing | Sort enabled alarms by next occurrence/time; make repeat days, timezone mode and next fire time explicit. Edits are confirmed, versioned, reversible, and never silently altered. | UI and repository tests for sort order, edit/save/cancel/delete/disable and scheduled-time consistency. |
| Paid access is discovered after setup or appears to block expected tasks | Explain Free vs Pro before a long setup flow. Let a new user create and test a basic alarm without an account or subscription. Expiry, restore failure, store outage and offline state must never disable the basic alarm. | First-run and purchase-state E2E checks; verify the basic local alarm remains editable and scheduled in each entitlement state. |
| Ads, review prompts or other overlays block dismissal while the alarm is ringing | No ad, review prompt, sign-in, network request, or paywall may interrupt ringing, mission completion, snooze, or emergency stop. | Wake-flow tests assert those screens do not appear before the session is safely completed or stopped. |
| Streaks/progress are motivating, but inaccurate history loses trust | Persist dated wake outcomes locally before presenting streaks or charts. Do not infer a successful wake from a notification tap or a test alarm. | Reboot/offline/duplicate-event tests for dated session history and streak calculation. |

## Scope and order

These findings refine the existing MVP and release plan; they do not move design ahead of the reliability and mission gates.

1. **Now:** finish cross-platform alarm reliability and validate it on real iOS and Android devices. The user's iPhone push-only test is not a passing alarm test.
2. **Next:** finish Math, Steps and QR mission runtime, permission recovery, retry, fallback, snooze and emergency stop on both platforms.
3. **Then:** production local persistence and six-language wiring/validation (English, Russian, Portuguese-Brazil, French, German and Spanish).
4. **Then:** account-free onboarding and transparent Free/Pro boundaries; backend/sync must remain outside the ringing and mission-critical wake path.
5. **Later:** progress/streak UI backed by real persisted history; evaluate photo/object missions, social accountability and sleep features only after reliability and privacy/consent trade-offs are specified.

## Source links

- [Erly App Store listing supplied by the product owner (Georgia storefront, Russian UI parameter)](https://apps.apple.com/ge/app/erly-wake-up-early/id6751428380?l=ru)
- [Erly Google Play listing](https://play.google.com/store/apps/details?id=com.erly.myapp)
- [Alarmy Google Play listing and visible reviews](https://play.google.com/store/apps/details?id=droom.sleepIfUCan)
- [Alarmy App Store listing and reviews](https://apps.apple.com/us/app/alarmy-loud-alarm-clock/id1163786766)
- [I Can't Wake Up! Google Play listing and visible reviews](https://play.google.com/store/apps/details?id=com.kog.alarmclock)
