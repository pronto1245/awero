# AWERO Product UX Design — approved direction

Status: **approved visual direction, partially implemented**.

The approved visual direction is based on the four-screen concept board in [the design image](awero-product-ux-concept.jpg). It is a visual baseline, not a literal screenshot of the current app. The implemented alarm screens now use warm ivory surfaces, deep navy text, sunrise coral actions, and restrained sage accents on iOS and Android.

## User value

AWERO helps people prepare for the morning, choose a short wake-up mission, and understand how their wake sessions are going. Alarm setup should remain easy, and progress must use only outcomes the app actually measured.

## Screens and implementation status

1. **Home / alarms — implemented.** Greeting, truthful next alarm, alarm list, enabled state, mission summary, add, edit, test, retry, and delete actions.
2. **Create / edit alarm — implemented.** Time, repeat days, Math/Steps/QR mission selection, difficulty, QR setup, permission explanation, save, and scheduling behavior.
3. **Wake / mission — implemented.** Ringing, selected mission, retry/fallback, snooze, completion, and emergency-stop states stay connected to the existing local wake flow.
4. **Progress — deferred to Phase 8.** Persisted aggregate statistics exist, but dated daily history does not. Do not show a weekly chart, streak, or sample score until the supporting data is implemented.

## Visual direction

- Warm ivory surfaces, deep navy text, sunrise coral for primary actions, and restrained sage for positive progress.
- Clear type hierarchy, large time and mission content, rounded cards, comfortable touch targets, and strong contrast.
- Keep wake screens focused and legible. The active wake screen currently uses the light palette; low-light behavior remains a separate design check.
- User-facing copy is localized in English, Russian, Brazilian Portuguese, French, German, and Spanish. Locale walkthrough and full accessibility acceptance remain part of Phase 6.

## Acceptance criteria

- Implemented screens keep create, edit, delete, test, mission, snooze, emergency-stop, and fallback actions connected to their current implementations.
- Alarm state and progress come from stored app data; no sample streak or score appears for real users.
- Empty states explain what the user can do next.
- Accessibility, Dynamic Type, and empty/populated screen walkthroughs are verified before Phase 6 closes.

## Current implementation boundary

Home, alarm setup, and wake screens are implemented on both platforms and their current CI gates are green. This does not close Phase 6: accessibility and locale walkthroughs remain, and progress stays deferred to Phase 8. Physical-device alarm validation remains in Phase 10.
