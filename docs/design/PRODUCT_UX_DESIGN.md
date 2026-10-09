# AWERO Product UX Design — approved direction

Status: **approved visual direction, partially implemented**.

The approved visual direction is based on the four-screen concept board in [the design image](awero-product-ux-concept.jpg). It is a visual baseline, not a literal screenshot of the current app. The implemented alarm screens now use warm ivory surfaces, deep navy text, sunrise coral actions, and restrained sage accents on iOS and Android.

## User value

AWERO helps people prepare for the morning, choose a short wake-up mission, and understand how their wake sessions are going. Alarm setup should remain easy, and progress must use only outcomes the app actually measured.

## Screens and implementation status

1. **Home / alarms — implemented.** Greeting, truthful next alarm, alarm list, enabled state, mission summary, add, edit, test, retry, and delete actions.
2. **Create / edit alarm — implemented.** Time, repeat days, Math/Steps/QR mission selection, difficulty, QR setup, permission explanation, save, and scheduling behavior.
3. **Wake / mission — implemented.** Ringing, selected mission, retry/fallback, snooze, completion, and emergency-stop states stay connected to the existing local wake flow.
4. **Settings — implemented.** The approved gear control opens a localized screen with the device language and a direct link to phone app-permission settings.
5. **Progress — deferred to Phase 8.** Persisted aggregate statistics exist, but dated daily history does not. Do not show a weekly chart, streak, or sample score until the supporting data is implemented.

## Visual direction

- Warm ivory surfaces, deep navy text, sunrise coral for primary actions, and restrained sage for positive progress.
- Clear type hierarchy, large time and mission content, rounded cards, comfortable touch targets, and strong contrast.
- Keep wake screens focused and legible. The active wake screen currently uses the light palette; low-light behavior remains a separate design check.
- Localization resources and automated key-parity checks cover English, Russian, Brazilian Portuguese, French, German, and Spanish. The user reports the iPhone walkthrough/manual checks are complete; do not request or repeat them. Source audit still identifies hard-coded active alarm-delivery strings and accessibility/localization implementation gaps that must be fixed before Phase 6 acceptance.

## Acceptance criteria

- Implemented screens keep create, edit, delete, test, mission, snooze, emergency-stop, and fallback actions connected to their current implementations.
- Alarm state and progress come from stored app data; no sample streak or score appears for real users.
- Empty states explain what the user can do next.
- Accessibility, Dynamic Type, and empty/populated screen walkthroughs are verified before Phase 6 closes.

## Current implementation boundary

Home, alarm setup, wake/mission, and Settings have implementations on both platforms; [CI run 37928994590](https://github.com/pronto1245/awero/actions/runs/37928994590) passed all four jobs. Phase 6 remains active because code-level localization/accessibility gaps remain. The user reports the iPhone walkthrough/manual checks are already complete, so do not repeat them. Progress stays deferred to Phase 8; the broader physical alarm reliability matrix remains in Phase 10.
