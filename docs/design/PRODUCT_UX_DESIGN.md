# AWERO Product UX Design — approved direction

Status: **approved visual direction; visual implementation is partial and is tracked in Phase 7**. See [Stage 7 Visual Implementation Plan](STAGE_7_VISUAL_IMPLEMENTATION_PLAN.md).

The approved visual direction is based on the four-screen concept board in [the design image](awero-product-ux-concept.jpg) and, per screen, on the owner's detailed reference screens below, which take precedence over the board. Treat its composition, image treatment, type hierarchy, colors, card shapes, spacing, and button styling as the acceptance reference on both platforms. Native controls may differ only where platform behavior or accessibility requires it; those adaptations must preserve the approved visual hierarchy.

## User value

AWERO helps people prepare for the morning, choose a short wake-up mission, and understand how their wake sessions are going. Alarm setup should remain easy, and progress must use only outcomes the app actually measured.

## Owner-approved corrections to the board (2026-10-10)

Where these differ from the concept board, these win:

1. **Button contrast.** Primary buttons, selected day chips and the ✓ key use a darker coral (about `#E0502F`) with white text. The bright coral (`#FF684B`) stays for backgrounds, artwork and accents; white on it is about 2.9:1 and fails contrast.
2. **Secondary text.** Grey captions on ivory (repeat days, mission descriptions, the emergency-stop hint) are darkened one to two steps to meet contrast.
3. **Dim wake variant.** The ringing/mission screen gets a dimmed variant (navy background, same coral) for dark bedrooms; other screens stay light.
4. **Alarm readiness.** Every alarm card shows whether it is scheduled or needs action/permissions.
5. **Edit and delete.** Tapping an alarm card edits it; a long press opens edit, test and delete (delete asks for confirmation). Long press is used instead of swipe because it works the same on both platforms and inside scrolling content; screen readers get the same three actions.
6. **Progress copy.** Average time reads as minutes to get up (e.g. «4,8 мин — среднее время подъёма»), not a star rating; the second tile shows the successful-wake percentage instead of repeating the streak; Russian uses a decimal comma.
7. **Snooze.** None of the reference wake screens shows snooze; it is removed from the wake screen when that screen is rebuilt.
8. **Imagery.** The owner's illustrated mountain-lake scenes, in the flat style of the reference screens, replace the code-drawn sunrise (see Scene illustrations).

## Reference screens (owner, 2026-10-10)

The binding per-screen targets are in [`screens/`](screens): [Home](screens/home.png), [New alarm](screens/create-alarm.png), [Wake — Math](screens/wake-math.png), [Wake — Steps](screens/wake-steps.png), [Wake — QR](screens/wake-qr.png), [Progress](screens/progress.png) and [Profile](screens/profile.png).

Dark-theme counterparts are in [`screens/dark/`](screens/dark): [Home](screens/dark/home.png), [New alarm](screens/dark/create-alarm.png), [Wake — Math](screens/dark/wake-math.png), [Wake — Steps](screens/dark/wake-steps.png), [Progress](screens/dark/progress.png) and [Profile](screens/dark/profile.png). The dark theme follows the system appearance across the whole app and uses the night scene; it replaces the narrower "dim wake variant" of correction 3.

They are taken literally except for these owner-approved points:

- **Steps:** the hint "tap the circle to simulate steps" is a prototype aid; real steps come from the motion sensor.
- **QR:** "camera feed" is a placeholder for the live camera preview.
- **Profile identity:** the app has no account name. Show initials and a name only if the person entered one; otherwise a neutral "My profile".
- **Weather chips** (Home, Math) need a real provider and location consent (Phase 9). Until then they are hidden, never filled with sample values.
- **Progress copy** follows correction 6 above, not the star rating.
- **New alarm actions:** "Проверить будильник" saves the alarm and immediately starts a test ring; a "Готово" action in the header saves without a test.
- Profile settings (melody, gradual volume, vibration, saved QR codes, sleep reminder) are product features, implemented in their own slices; they are not mocked in the UI before they work.

## Design tokens and themes

Measured from the reference screens. The app follows the system appearance (light or dark) on both platforms; screens use these tokens, never literal colors. Code: `apps/ios/AWERO/App/AweroDesign.swift`, `apps/android/app/src/main/java/app/awero/ui/AweroDesign.kt`.

| Token | Role | Light | Dark |
|---|---|---|---|
| `ivory` | Page background | `#FBF6EF` | `#121624` |
| `navy` | Primary text | `#1D2433` | `#F2EDE6` |
| `textSecondary` | Secondary text | `#6E6B6B` | `#A39D97` |
| `surface` | Cards, lists, keypad keys | `#FFFFFF` | `#1E2335` |
| `surfaceMuted` | Time wheel, stat tiles | `#F4ECE2` | `#1E2335` |
| `surfaceStrong` | Time wheel selection band | `#ECE2D6` | `#2B3247` |
| `chip` | Unselected chips, switch tracks | `#EFE8DF` | `#262C40` |
| `coral` | Accent: icons, outlines, selected tab | `#F26A3D` | `#F26A3D` |
| `coralStrong` | Fills with white text: buttons, day chips, ✓ | `#E0502F` | `#E0502F` |
| `coralSoft` | Selected mission card | `#FDEEE6` | `#2E2326` |
| `sage` / `successSoft` / `successText` | Completed days, stability card | `#6AA84F` / `#E9F0DF` / `#3F8A3A` | `#6AA84F` / `#1F2E24` / `#7FC36A` |
| `surfaceWarm` | Tip cards | `#FDF0DC` | `#2E2A1F` |
| `warning` / `warningSoft` / `warningLine` | Emergency stop, destructive | `#B5241D` / `#FBE9E5` / `#F2C9C2` | `#F0645A` / `#2E1F22` / `#5A2C2C` |
| `sun` | Sun glyphs | `#F5A623` | `#F5A623` |

White on `coral` is about 3.0:1, enough only for large bold labels; white on `coralStrong` is about 3.9:1, so every filled control with a white label uses `coralStrong`.

## Scene illustrations

Pages use one illustrated mountain-lake scene at several times of day, supplied by the owner (853×1844 portraits; `apps/ios/AWERO/App/Scenes`, `apps/android/app/src/main/res/drawable-nodpi/scene_*`). As in the reference screens, the scene is a full-screen background behind the page: the sun and peaks sit behind the greeting and the next-alarm card overlaps them, and the artwork fades into the ivory surface at the bottom. In the light theme Home follows device local time — dawn 05–11, day 11–18, sunset 18–05 — and the wake and mission screens always use dawn. The dark theme uses the night scene on every page; its lower part is recolored to fade into the dark page background instead of ivory.

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
