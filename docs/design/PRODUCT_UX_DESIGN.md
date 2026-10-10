# AWERO Product UX Design — binding target

Status: **binding target set by the owner on 2026-10-10; partially implemented.**

The four-screen board in [the design image](awero-product-ux-concept.jpg) is the **required layout**, not a loose direction. Screens are implemented to match its structure, components, copy and palette, with the owner-approved corrections listed below. Where the board and this document differ, this document wins. Any deviation from the board needs an explicit owner decision recorded here.

The owner prioritized UX parity with this board ahead of Phase 7 (billing). Reliability rules still apply: no screen may block ringing, mission completion, snooze or emergency stop, and no screen may show fabricated data.

## Owner-approved corrections to the board

1. **Button contrast.** Primary buttons, selected day chips and the ✓ key use the darker coral `#E0502F` with white text. The bright coral `#FF684B` stays for backgrounds, illustrations and accents only (white on `#FF684B` is about 2.9:1 and fails contrast).
2. **Secondary text.** Grey captions on ivory ("Будни · Математика", mission descriptions, "Только в крайнем случае") are darkened by one to two steps so they meet contrast on ivory.
3. **Dark wake screen.** The ringing/mission screen gets a dimmed variant (navy background, same coral) for dark bedrooms. Other screens stay light.
4. **Alarm readiness.** Each alarm card shows a small status line or icon: scheduled, or action/permission required. This is mandatory; it is the product's reliability promise.
5. **Edit and delete.** Tapping an alarm card opens edit; swipe deletes with confirmation. Test/Edit/Delete buttons are removed from the cards.
6. **Progress copy.** "4,8 мин — среднее время подъёма" (not a star rating); the second tile shows the successful-wake percentage instead of repeating the streak; Russian uses a decimal comma.
7. **Snooze** and **Profile contents** — open owner decisions; until decided, keep the existing snooze control on the wake screen and do not ship a Profile tab with placeholder content.

## Required screens

| Screen | Required elements (from the board) | Status |
|---|---|---|
| Home | AWERO wordmark + gear; greeting and subtitle; sunrise-over-mountains illustration; "Следующий будильник" card with large time, "Завтра, Пн · Математика" and › chevron; "Мои будильники" list with sun/shoe icons, time, "Будни · Математика", coral switch, readiness status; coral "Добавить будильник"; bottom tab bar | Partial: palette, copy, list and next alarm exist; illustration, chevron, icons, card layout and tab bar missing |
| New alarm | Back chevron + centered title; wheel time picker in a card; round day chips Пн–Вс (selected = coral); three mission cards with icon, title and one-line description; "Проверить будильник" primary action; time zone moved under an "Advanced" disclosure | Partial: all functions exist; layout and components differ |
| Wake / mission | Sunset-landscape background (dark variant per correction 3); ✕ control; "Пора просыпаться" + "Решите пример, чтобы выключить будильник"; problem in a white card; custom 3×4 keypad with ⌫ and ✓; "Экстренно выключить / Только в крайнем случае" card; the mission appears immediately | Partial: flow, missions and emergency stop work; visual layout differs |
| Progress | Streak header; stability card; weekly bars Пн–Вс from real wake history; two stat tiles; tip card; tab bar | Not started; requires dated daily wake history first |
| Settings | Reachable from the gear; device language; link to system permissions | Implemented |

## Data rules

- Alarm state and progress come only from stored app data. No sample streak, score or chart for real users.
- The weekly chart and streak require a persisted per-day wake history (date, result, time to get up). Build that store before the Progress screen.
- Empty states explain what the user can do next.

## Localization and accessibility

All user-facing strings, including the native Android alarm host and wake error messages, come from `packages/localization/*.json` in English, Russian, Brazilian Portuguese, French, German and Spanish. Dynamic Type / large text and VoiceOver / TalkBack labels are required on every new component. Physical-device screen-reader checks remain in Phase 10.

## Implementation order

1. Shared components per platform: palette tokens, primary button, white card, round day chip, mission card, numeric keypad, tab bar, illustrations.
2. Wake / mission screen.
3. New / edit alarm screen.
4. Home screen.
5. Daily wake history store, then Progress screen and tab bar.
6. Profile, after the owner defines its contents.
