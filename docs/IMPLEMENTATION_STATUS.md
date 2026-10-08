# AWERO Implementation Status

## 1. Product foundation
- [x] Product vision, JTBD, MVP scope and roadmap
- [x] Reliability-first / local-first principles
- [x] Successful Wake Rate as North Star

## 2. Repository / architecture
- [x] Monorepo structure
- [x] NestJS backend foundation
- [x] PostgreSQL schema foundation
- [x] API contracts foundation
- [x] Shared types
- [x] Localization package foundation
- [x] CI foundation
- [ ] Production CI build verification

## 3. Alarm engine
- [x] Alarm model
- [x] Versioned scheduling
- [x] Cancel/recreate on edit
- [x] Recurring weekdays
- [x] Device-local timezone
- [x] Fixed timezone
- [x] DST-aware wall-clock calculation
- [x] Test alarm
- [x] Pending schedule verification
- [x] Automatic repair
- [x] Android boot/time/timezone recovery
- [ ] Physical-device reliability validation
- [ ] iOS Xcode project/signing validation

## 4. Wake flow
- [x] Wake session model
- [x] Triggered → mission → completed state flow
- [x] Snooze state
- [x] Emergency stop state
- [x] Session persistence prototype
- [x] Android wake activity
- [x] iOS wake screen
- [ ] Production-grade OS snooze rescheduling
- [ ] Physical-device wake validation

## 5. Missions
- [x] Math mission logic
- [x] Steps runtime foundation
- [x] QR runtime foundation
- [x] Android ML Kit QR decoding
- [x] Mission fallback policy
- [ ] Mission UI for Math
- [ ] Mission UI for Steps
- [ ] Mission UI for QR
- [ ] Retry / timeout UX
- [ ] Runtime fallback transitions
- [ ] Photo AI mission (V2)
- [ ] Mixed mission (V2)

## 6. Local data
- [x] Prototype local alarm storage
- [x] Prototype wake-session storage
- [x] Statistics model
- [x] Adaptive policy
- [ ] Room production storage
- [ ] Core Data/SQLite production storage
- [ ] Persistent statistics/streak UI

## 7. Onboarding / localization
- [x] Anonymous first-run mode foundation
- [x] Permission education/runtime requests
- [x] First alarm flow foundation
- [x] Test alarm flow
- [x] en
- [x] ru
- [x] pt-BR
- [x] fr
- [x] de
- [x] es

## 8. Backend / sync
- [x] Auth session foundation
- [x] Anonymous account
- [x] Device registration
- [x] Alarm CRUD API
- [ ] Wake session API
- [ ] Statistics API
- [ ] Analytics ingestion
- [ ] Offline sync queue
- [ ] Conflict resolution
- [ ] Support tickets

## 9. Monetization
- [ ] iOS StoreKit 2
- [ ] Android Google Play Billing
- [ ] Server entitlement validation
- [ ] Restore purchases
- [ ] Free/Pro gating
- [ ] Paywall
- [ ] Subscription expiry safety

## 10. AI
- [x] Basic local adaptive policy
- [ ] AI profile API
- [ ] AI recommendation API
- [ ] JSON schema validation
- [ ] Policy engine
- [ ] Recommendation feedback
- [ ] Explainable reason codes

## 11. QA
- [ ] Unit tests
- [ ] Integration tests
- [ ] E2E tests
- [ ] Offline tests
- [ ] Reboot tests
- [ ] Timezone/DST tests
- [ ] Permission-denied tests
- [ ] Subscription tests
- [ ] Physical iOS test
- [ ] Physical Android test

## Release gate

AWERO must **not** be described as production-ready until the physical iOS and Android release candidates pass the critical E2E matrix and the release checklist.

Current status: **MVP implementation in progress; repository contains foundation and runtime scaffolding, not a verified production build.**


## Reliability Foundation

Status: CODE + CI VERIFIED


## Local Persistence Hardening

The approved ten-point **automated local persistence gate** has a complete successful CI baseline. Both platforms have crash/restart checks; Android additionally exercises real process SIGKILL and AlarmManager recovery. Core Data failure handling, Room 1→2 schema validation, sync retry deduplication, wake-flow restore and local E2E are covered.

The verification matrix, workflow evidence and physical-device boundary are recorded in [Local Persistence Hardening](LOCAL_PERSISTENCE_HARDENING.md). Backend work is excluded from this gate.
