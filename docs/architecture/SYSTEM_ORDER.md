# AWERO System Implementation Order

## 1. Reliability foundation
- Local alarm is source of truth.
- Versioned scheduling.
- Recurrence.
- Timezone and DST.
- Reboot/time-change recovery.
- Test alarm.
- Snooze.
- Emergency stop.

## 2. Mission runtime
- Math validation.
- Steps validation.
- QR validation.
- Permission failures.
- Retry.
- Fallback.
- Technical failures never count as user failure.

## 3. Local persistence
- Android Room.
- iOS Core Data/SQLite abstraction.
- Transactional wake-session events.
- Durable sync queue.
- Crash recovery.

## 4. Backend
- Anonymous auth.
- Device registration.
- Alarm CRUD.
- Wake sessions/events.
- Statistics.
- Sync.
- Analytics.
- Support diagnostics.

## 5. Product UX
- Onboarding <= 60 sec.
- First alarm without account.
- Test before trust.
- Alarm list/edit/delete/enable-disable.
- Statistics.
- Streak.
- Failure/support flow.
- Use the complete cross-platform feature scope and acceptance rules in [Product Requirements](../PRODUCT_REQUIREMENTS.md).
- Use competitor and review findings as acceptance inputs: [Competitor and Review Gap Analysis](../research/COMPETITOR_AND_REVIEW_GAP_ANALYSIS.md).
- Keep all planned competitor capabilities tracked, including optional weather/morning briefing and exercise missions; sequence them after the reliability and core mission gates.
- Visual direction and screen acceptance criteria: [Product UX Design](../design/PRODUCT_UX_DESIGN.md) (planned; not implemented).
- Keep sort order and alarm edits predictable; explain repeat/timezone/next fire.
- Never place ads, review prompts, login, paywall, or network-dependent flow in the ringing/mission path.
- Preserve a usable basic alarm with no account or subscription; explain Free/Pro boundaries before setup friction.

## 6. Monetization
- Free entitlement always retains basic alarm.
- StoreKit 2.
- Google Play Billing.
- Backend receipt validation.
- Restore purchase.
- Subscription expiry cannot break the basic alarm.

## 7. Adaptive logic and AI
- Local adaptive policy first.
- Server AI recommendation second.
- JSON schema validation.
- Policy Engine.
- AI never controls alarm time, permissions, subscription, timezone or emergency controls.

## 8. Release
- Unit.
- Integration.
- E2E.
- Offline.
- Reboot.
- DST/timezone.
- Permission denied.
- Camera/motion unavailable.
- Subscription expiry.
- Device migration.
- Store review gates.

No layer is considered production-complete until the preceding layer has passed its tests.
