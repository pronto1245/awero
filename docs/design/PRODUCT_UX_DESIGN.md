# AWERO Product UX Design — approved direction

Status: **approved visual direction; visual implementation is partial and is tracked in Phase 7**. See [Stage 7 Visual Implementation Plan](STAGE_7_VISUAL_IMPLEMENTATION_PLAN.md).

The approved visual direction is based on the four-screen concept board in [the design image](awero-product-ux-concept.jpg). Treat its composition, image treatment, type hierarchy, colors, card shapes, spacing, and button styling as the acceptance reference on both platforms. Native controls may differ only where platform behavior or accessibility requires it; those adaptations must preserve the approved visual hierarchy.

## User value

AWERO helps people prepare for the morning, choose a short wake-up mission, and understand how their wake sessions are going. Alarm setup should remain easy, and progress must use only outcomes the app actually measured.

## Screens and implementation status

1. **Home / alarms — functional screen exists; visual parity is not yet accepted.** Greeting, truthful next alarm, alarm list, enabled state, mission summary, add, edit, test, retry, and delete actions.
2. **Create / edit alarm — functional screen exists; visual parity is not yet accepted.** Time, repeat days, Math/Steps/QR mission selection, difficulty, QR setup, permission explanation, save, and scheduling behavior.
3. **Wake / mission — functional paths exist; visual parity across runtime entry points is not yet accepted.** Ringing, selected mission, retry/fallback, snooze, completion, and emergency-stop states stay connected to the existing local wake flow.
4. **Settings — functional screen exists; visual parity is not yet accepted.** The approved gear control opens a localized screen with the device language and a direct link to phone app-permission settings.
5. **Progress — visual screen belongs to Phase 7; populated data belongs to Phase 9.** Implement an honest empty state now. Do not show a weekly chart, streak, or score until backed by persisted dated history.

## Visual direction

- Warm ivory surfaces, deep navy text, sunrise coral for primary actions, and restrained sage for positive progress.
- Clear type hierarchy, large time and mission content, rounded cards, comfortable touch targets, and strong contrast.
- Keep wake screens focused and legible. The active wake screen currently uses the light palette; low-light behavior remains a separate design check.
- Localization resources and automated key-parity checks cover English, Russian, Brazilian Portuguese, French, German, and Spanish. Phase 6 now uses the complete locale JSON dictionaries as the source for generated Android/iOS resources; first-run onboarding, active alarm error copy, and native accessibility semantics are implemented. The user reports the iPhone walkthrough/manual checks are complete; do not request or repeat them. Physical VoiceOver/TalkBack use remains a release-device check.

## Acceptance criteria

- Implemented screens keep create, edit, delete, test, mission, snooze, emergency-stop, and fallback actions connected to their current implementations.
- Alarm state and progress come from stored app data; no sample streak or score appears for real users.
- Empty states explain what the user can do next.
- Accessibility, Dynamic Type, and empty/populated screen walkthroughs are verified as part of Phase 7 visual acceptance.

## Current implementation boundary

Home, alarm setup, wake/mission, and Settings have functional implementations on both platforms; [CI run 37928994590](https://github.com/pronto1245/awero/actions/runs/37928994590) passed all four jobs, and Phase 6 localization/onboarding acceptance is separately recorded in the status matrix. This document defines visual direction; current work and acceptance are governed by [Implementation Status](../IMPLEMENTATION_STATUS.md). Phase 7 now covers full visual implementation and screenshot acceptance. Progress visuals begin with an honest empty state in Phase 7; populated history depends on Phase 9. The user reports the iPhone walkthrough/manual checks are complete; do not repeat them. The broader physical alarm reliability matrix remains in Phase 11.
