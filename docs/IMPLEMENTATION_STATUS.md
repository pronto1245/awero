# AWERO Implementation Status

## Mobile foundation
- [x] Monorepo
- [x] iOS SwiftUI source foundation
- [x] Android Kotlin foundation
- [x] Alarm model
- [x] Versioned scheduling
- [x] Recurring weekdays
- [x] Timezone modes
- [x] DST-aware wall-clock scheduling
- [x] Test alarm
- [x] Alarm verification
- [x] Automatic recovery
- [x] Android boot/time/timezone recovery
- [x] Wake flow entry point
- [x] Math mission foundation
- [x] Wake session foundation
- [ ] Real iOS Xcode project/signing
- [ ] Android full Gradle dependency setup
- [ ] Steps runtime
- [ ] QR runtime
- [ ] Fallback UI/runtime
- [ ] Snooze UI/runtime
- [ ] Emergency Stop UI
- [ ] Local database production layer
- [ ] Full statistics UI

## Rule
Do not call the app production-ready until the remaining unchecked mobile runtime items and critical E2E tests pass on physical iOS and Android devices.
