# AWERO Phase 7 visual implementation report

**Status: visual acceptance is pending on the corrected branch head.** The approved reference is `awero-product-ux-concept.jpg` and the matching four-screen board supplied by the product owner. This report deliberately does not claim completion until current-head CI screenshots have been reviewed on both platforms.

## Scope and source integrity

- Work continues on `codex/stage-7-visual-design`, based on PR #1; `main` has not been changed and the PR has not been merged.
- The branch compared with current `main` has no deleted paths. Its changes are confined to the Phase 7 visual implementation, localization, screenshot/acceptance coverage, and the CI steps needed to export that evidence.
- Native alarm scheduling, persisted alarm/session models, and the existing wake-flow architecture remain the integration boundary.
- The four approved screens are Home, Create/Edit, active Wake/Math, and Progress. Onboarding, Settings, mission variants, and recovery states use the same visual system without inventing unsupported product data.

## Corrections in the current working change

- Rework Home and the create/edit composition around the approved wordmark, sunrise treatment, upcoming-alarm card, weekday selector, mission cards, and pinned primary action.
- Replace the previous Wake composition with the approved localized heading/instruction, scenic top area, visible close/emergency action, expression card, and keypad.
- Keep Progress honest and empty until Phase 9 provides real dated history; its create-alarm action remains functional.
- Add matching mission descriptions and wake/no-enabled-alarm strings for en, ru, pt-BR, fr, de, and es, then regenerate native resources and require the keys in the locale parity validator.
- Add screenshot/UI coverage for mission variants, six locales, and large text; keep alarm data and scheduling paths unchanged.

## Checks completed locally

- Native locale resources are generated and in sync.
- Locale parity passes for en, ru, pt-BR, fr, de, es.
- Swift source and iOS UI-test syntax parsing passes.
- iOS app build and `build-for-testing` succeed for the simulator target.
- `git diff --check` passes.
- No local iOS simulator runtime or Android Gradle installation is available in this workspace. Therefore local checks do not replace current-head simulator/emulator tests or screenshot review.

## Still required before acceptance

1. Commit and push this correction set to the existing PR branch.
2. Require all current-head CI jobs to pass; inspect the Android and iOS screenshot artifacts from that same head, including the long German create screen, Math invalid state, large text, and all six locales.
3. Compare those captures with the approved board, fix any remaining material mismatch, and rerun CI on the resulting head.
4. Record the exact current-head CI run and screenshot artifact in the PR and final report.

The prior `Create-de.png` and `MathInvalid.png` examples are not accepted: they show the old clipped/oversized create screen and the old English/flat-background Math layout. A green historical CI run does not change that verdict.

## Product boundaries

- Progress charts/streak metrics remain intentionally empty until Phase 9 supplies persisted dated history and metric rules.
- This phase's CI screenshot evidence is not a new physical-device test of audible alarm delivery, store readiness, VoiceOver/TalkBack, or the Phase 11 device matrix.
- Do not mark Phase 7 complete until the remaining acceptance steps above pass. Do not merge PR #1 or update `main` without the owner's separate approval.
