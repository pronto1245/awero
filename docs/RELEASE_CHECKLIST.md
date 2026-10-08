# AWERO Release Checklist

## Mobile
- [ ] Generate `apps/ios/AWERO.xcodeproj` with XcodeGen and build the app for iOS Simulator
- [ ] Install the signed development build on a physical iPhone from Xcode
- [ ] Verify AlarmKit authorization, lock-screen alarm sound and Start mission action on iOS 26+
- [ ] Verify UserNotifications fallback and sound on iOS 17–25
- [ ] Verify fixed-timezone notification fallback on iOS 26+
- [ ] Verify iOS local notification delivery while AWERO is terminated and offline
- [ ] Verify iOS active wake mission restores after force-quit and relaunch
- [ ] Verify edited/deleted iOS alarms do not deliver stale notifications
- [ ] Reboot the iPhone, unlock and relaunch AWERO; verify alarms remain and reconcile
- [ ] Build on physical iPhone
- [ ] Build on physical Android
- [ ] Alarm fires with app terminated
- [ ] Alarm fires offline
- [ ] Alarm survives reboot
- [ ] Timezone change tested
- [ ] DST transition tested
- [ ] Edit alarm does not fire stale version
- [ ] Test alarm works
- [ ] Math mission works
- [ ] Steps permission/availability works
- [ ] QR permission/scanning works
- [ ] Mission retry works
- [ ] Fallback works
- [ ] Snooze policy works
- [ ] Emergency stop works
- [ ] Wake session persisted
- [ ] Statistics updated
- [ ] Localization verified in all six languages

## Backend
- [ ] PostgreSQL migrations applied to staging
- [ ] Auth tested
- [ ] Anonymous conversion tested
- [ ] Alarm CRUD tested
- [ ] Sync conflict tests
- [ ] Subscription receipt validation
- [ ] AI schema/policy tests
- [ ] Rate limits and audit logs verified

## Store
- [ ] App Store metadata
- [ ] Google Play metadata
- [ ] Privacy policy
- [ ] Terms
- [ ] Subscription disclosures
- [ ] Restore purchases
- [ ] Support contact
