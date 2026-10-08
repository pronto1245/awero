# AWERO iOS

Native SwiftUI iOS app. The Xcode project is generated from `project.yml` with XcodeGen.

## Requirements

- macOS with Xcode installed
- XcodeGen (`brew install xcodegen`)
- iOS 17 or later
- An Apple ID configured in Xcode to install on a physical iPhone

## Generate and open the Xcode project

From the repository root:

```sh
cd apps/ios
xcodegen generate --spec project.yml
open AWERO.xcodeproj
```

The project uses bundle identifier `app.awero`, includes every Swift source under `AWERO/`, and declares the camera and motion permission descriptions required by QR and step missions.

## Install on an iPhone

1. Connect the iPhone to the Mac, unlock it, and accept the Trust This Computer prompt.
2. In Xcode, open **Settings → Accounts** and add the Apple ID that will sign the development build.
3. Select the **AWERO** project and app target. Under **Signing & Capabilities**, turn on **Automatically manage signing** and select the Apple development team.
4. If Xcode reports that `app.awero` cannot be registered, change **Bundle Identifier** to a unique value for the Apple team.
5. Select the connected iPhone as the run destination and press **Run**. On first use, allow notifications. If iOS asks to enable Developer Mode, follow the prompt, restart the phone, and run the app again.

## Physical-device persistence and alarm check

1. In AWERO, create an alarm for 3–5 minutes from now and save it.
2. On iOS 26+, allow AlarmKit when prompted. Lock the iPhone and wait for the system alarm. Verify the alarm sound and alert, tap **Start mission**, and check that AWERO opens the wake flow. On iOS 17–25, verify the local notification fallback separately.
3. Create a second alarm, close AWERO from the app switcher, and wait for its notification. This checks scheduled delivery while the app process is terminated.
4. Tap **Test** on an alarm. The test notification is scheduled 30 seconds later. Tap the notification, start the mission, force-quit AWERO, then reopen it. The active mission should be restored.
5. Create an alarm, edit its time, and confirm the old time does not alert. Delete an alarm and confirm it no longer alerts.
6. For reboot coverage, create an alarm far enough ahead to allow a restart, reboot the iPhone, unlock it, and open AWERO. Confirm the alarm is still listed and wait for its scheduled alert. Record whether the alert arrived before opening AWERO as a separate observation.

Record the iPhone model, iOS version, notification permission state, alarm time, whether AWERO was open/backgrounded/terminated, and pass or fail for each step. iOS local notifications are delivered by the operating system; notification settings, Focus modes, silent mode, and device state can affect what is audible or visible.

The iOS CI job generates this project and builds the app for the simulator. A physical iPhone run still requires signing with the tester's Apple development team.
