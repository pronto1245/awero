# AWERO Phase 7 — Full visual design implementation

**Status:** implementation stage, based on the approved four-screen AWERO concept board in this repository and the matching reference supplied on 2026-10-10.  
**Platforms:** iOS and Android.  
**Architecture constraint:** retain SwiftUI, Compose, AlarmKit/UserNotifications, AlarmManager, existing alarm/session models, local persistence, and current coordinators. This phase changes presentation and navigation only unless a UI defect requires a narrowly scoped behavioral correction.  
**Governing product plan:** ../AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md  
**Current UI baseline:** PRODUCT_UX_DESIGN.md

## Goal

Bring every approved screen and state to a consistent, finished visual system on both platforms. Complete means the screen matches the approved design intent, connects to real app behavior and data, handles all required states, is localized and accessible, and has visual and automated acceptance evidence. A successful compile alone is not design acceptance.

## Design source and scope

The approved concept board contains four screens:

1. Home and alarm list, with upcoming alarm card and primary add action.
2. Create/edit alarm, with time, repeat days, mission choice and test action.
3. Active wake / Math mission, with wake instruction, answer controls and emergency stop.
4. Progress, with history-derived progress indicators.

The repository's docs/design/awero-product-ux-concept.jpg and the user-supplied matching board are the visual baseline. Any later design images from the owner must be compared and incorporated before the affected screen is considered final.

The board does not define onboarding, Settings, permission/error dialogs, Steps/QR runtime, terminal wake states, or empty states. Apply the same colors, type hierarchy, card geometry, spacing, icon treatment, and touch-target rules to those screens while preserving their existing behavior. Record platform-specific differences where system UI is required.

## Audit findings at phase start

- The palette is duplicated in screen files rather than expressed as a small platform design system.
- Home, create-alarm and wake screens exist on both platforms, but their composition is not yet a verified match to the board.
- The approved bottom tab navigation is not implemented consistently; the visible Settings gear is not a substitute for the board's Home / Progress / Profile navigation.
- A Progress screen is not implemented. Its example streak, week chart and 4.8 score cannot be shown as user data until backed by persisted dated wake sessions and an explicitly defined metric.
- iOS uses its native time picker and Android opens a platform time picker; preserve platform-native controls where they improve usability, but style the surrounding hierarchy consistently.
- Android has both a Compose wake screen and a legacy View-based WakeAlarmScreen/MissionRuntimeScreen path. Identify and document the runtime entry points; do not silently restyle an inactive screen or delete the OS-facing alarm Activity.
- The concept uses sunrise landscape artwork, but the board image is not an app asset. Use approved packaged artwork or a deliberate native illustration; do not rely on a remote image at alarm time.
- Existing visual implementation and green CI do not establish screenshot parity.

## Ordered implementation slices

### 7.1 Visual foundation
- Define matching semantic tokens on iOS and Android for ivory background, navy text, coral primary action, sage success, warning/error, spacing, corner radii, typography roles and minimum touch targets.
- Replace duplicated literals in touched screens with the platform token definitions.
- Define reusable button, surface/card, section-heading and status-banner treatments.
- Add a deterministic, bundled sunrise illustration treatment that is available offline and has a readable fallback.

**Gate:** token values and component behavior are documented; both app targets compile; contrast, Dynamic Type/large text and RTL-independent layout checks pass.

### 7.2 Home and navigation
- Match wordmark/header, greeting, sunrise treatment, upcoming alarm card, chronological alarm list and coral add button.
- Add consistent Home / Progress / Profile navigation on both platforms. Keep Settings reachable from Profile and retain current alarm management actions.
- Show truthful loading, empty, storage-error, no-enabled-alarm and populated states.
- Keep time, next fire, timezone, mission and schedule readiness sourced from the alarm store/scheduler.

**Gate:** create/edit/enable/disable/test/delete/retry actions remain connected; next alarm is sorted by its actual next occurrence; no mock alarms or fake schedule status.

### 7.3 Create/edit alarm
- Align title, time selection surface, weekday chips, mission cards, selected/disabled states and sticky primary action with the concept.
- Preserve timezone mode, mission difficulty, QR configuration, permission education and failure recovery even where the concept omits them.
- Use the same visual hierarchy for create and edit; present save/cancel and destructive behavior clearly.

**Gate:** every saved field round-trips through storage and OS scheduling; edit/cancel/back/delete regressions pass; all controls are localized and accessible.

### 7.4 Wake and mission
- Apply the approved focused wake composition to the runtime-active paths on both platforms.
- Match Math prompt and input affordance; style Steps and QR consistently; retain retry, timeout, fallback, snooze, completion and emergency stop.
- Keep emergency stop visible and operable during every active mission. No navigation tab, animation, artwork or styling can cover it.
- Preserve native lock-screen/system alarm surfaces and their platform limitations.

**Gate:** end-to-end trigger → mission → success/retry/fallback/snooze/emergency stop remains local, persisted and independent from network/account/paywall; target UI tests pass.

### 7.5 Progress presentation
- Build the Progress tab using real persisted wake-session data only.
- Until dated-history aggregation is available, show a designed honest empty state and explain that progress appears after completed real alarms. Do not render the concept's five-day streak, chart or 4.8 average from sample values.
- Define metric semantics (day boundary/timezone, missed vs technical outcome, test exclusion, streak calculation) with the history stage before showing populated values.

**Gate:** empty/error/populated states are distinct; values reconcile to persisted sessions; test alarms and technical failures are excluded according to approved semantics.

### 7.6 Profile, Settings, and system states
- Extend the visual system to onboarding, profile, Settings, permission education, schedule/storage errors, QR scanner, mission fallback and completed/stopped screens.
- Keep profile features that have no approved behavior clearly unavailable or omit them; do not add fake account, subscription or statistics controls.

**Gate:** no dead actions; all supported states have localized copy and recovery; the same visual tokens are used on both platforms.

### 7.7 Cross-platform visual and behavioral acceptance
- Capture the same representative states on iOS simulator and Android emulator at reference viewport sizes.
- Compare each screenshot with the approved board and document justified platform adaptations.
- Check all six locales (en, ru, pt-BR, fr, de, es), long text, 12/24-hour time, large text, accessibility labels, contrast, small screens and dark/low-light policy.
- Run focused UI tests and the complete CI workflow after scoped changes. Reuse already accepted physical iPhone checks; require a new device check only if a changed behavior demands it.

## Definition of done

- All four board screens exist on iOS and Android, including Progress with a truthful data-backed or empty state.
- The screen/state matrix below has no unreviewed differences from the approved reference.
- The active runtime path is verified for each platform; unused duplicate UI is not mistaken for shipped design.
- Design actions are real and preserve existing alarm, persistence and wake-flow invariants.
- Six locales, accessibility, small-screen/large-text behavior and error/empty states are verified.
- Screenshot evidence and automated UI/behavior tests pass; full CI is green.
- No claim of 100% is made for physical delivery, store readiness, or any untested device state based only on CI.

## Screen/state matrix

| Screen/state | iOS | Android | Data/action source | Required evidence |
|---|---|---|---|---|
| Home: empty/loading/error/populated | SwiftUI | Compose | AlarmStore / AlarmCoordinator + scheduler readiness | UI tests, six locales, screenshots |
| Create/edit: defaults, validation, save failure | SwiftUI | Compose | Existing alarm coordinator/store and native scheduler | Round-trip, cancel/back, permission and error tests |
| Wake: ringing, mission, retry/fallback, snooze, complete, emergency stop | SwiftUI/AlarmKit entry | Compose plus OS-facing Activity entry | Existing persisted WakeFlowController | UI/E2E tests on actual active entry paths |
| Math/Steps/QR | SwiftUI | Active Compose/View runtime | Existing mission runtimes | Valid/invalid, timeout, permission/offline fallback tests |
| Progress: empty/error/populated | SwiftUI | Compose | Persisted dated wake sessions | No fabricated data; aggregation reconciliation |
| Profile/Settings | SwiftUI | Compose | Existing settings only | No dead controls; localization/accessibility |
| Permission/schedule/storage states | Native UI | Native UI | OS permission + storage/scheduler readiness | Denial, retry, settings-return tests |

## Dependency and scope boundaries

- Progress visuals can be implemented with a real empty state in Phase 7. Populated streaks/charts depend on the history/statistics implementation, now Phase 9.
- Billing and Pro controls remain Phase 8. Do not add fake paywalls or Pro labels during the design phase.
- Physical release matrix remains Phase 11. CI screenshot checks do not prove audible alarm delivery.
- The full feature roadmap (photo/exercise/sequence/weather/sleep/social/AI) remains in its existing later phases.
