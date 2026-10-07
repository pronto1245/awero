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
