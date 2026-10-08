# AWERO Product UX Design — planned

Status: **design direction recorded; UI implementation has not started**.

The approved visual direction is based on the four-screen concept board in [the design image](awero-product-ux-concept.jpg). It is a redesign proposal, not a screenshot of the current app. The current iOS UI is a basic SwiftUI interface with a black background and English labels.

## User value

AWERO helps people prepare for the morning, choose a short wake-up mission, and see how their wake sessions are going. The app should make alarm setup easy and make progress understandable without claiming outcomes the app has not measured.

## Screens to design and implement

1. **Home / alarms** — greeting, next alarm, alarm list, enabled state, mission summary, and a clear add-alarm action. Keep edit, test, and delete actions available.
2. **Create / edit alarm** — time, repeat days, mission selection for Math, Steps, and QR, and a test action. Preserve the existing save and scheduling flows.
3. **Wake / mission** — focused wake-up state, readable mission prompt and controls, plus visible snooze and emergency-stop controls where the current session state allows them. Retain the existing mission and fallback behavior.
4. **Progress** — show only metrics available from persisted wake statistics, such as completed wakes, success rate, average completion time, snoozes, and fallback count. The current data does not contain dated daily history, so do not draw a weekly chart or streak until that data is implemented.

## Visual direction

- Warm ivory surfaces, deep navy text, sunrise coral for primary actions, and restrained sage for positive progress.
- Clear type hierarchy, large time and mission content, rounded cards, comfortable touch targets, and strong contrast.
- Keep wake screens focused and legible in low light; the light palette in the concept board is a starting point, not a requirement for the active wake state.
- Use Russian copy for the first implementation and keep strings ready for localization.

## Acceptance criteria

- The four screens share one visual system and work on iPhone screen sizes and Dynamic Type.
- Existing create, edit, delete, test, mission, snooze, emergency-stop, and fallback actions remain connected to their current implementations.
- Alarm state and progress shown in the UI come from stored app data; no sample values such as a five-day streak or 4.8 score appear for real users.
- Empty states explain what the user can do next.
- UI tests or previews cover the empty and populated alarm list, alarm setup, active mission, and empty/populated progress states.

## Current implementation boundary

This document records future Product UX work only. It does not change the current iOS or Android UI and does not claim that the concept's alarm experience is already available on device.
