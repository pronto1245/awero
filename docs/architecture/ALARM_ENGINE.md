# AWERO Alarm Engine v2

Versioning: each occurrence is identified by alarm id, version, and weekday. Editing increments version. The iOS notification identifier contains the version; Android carries version in the PendingIntent payload.

Recurrence: iOS uses repeating calendar notifications. Android uses exact one-shot RTC alarms for selected weekdays and schedules the next occurrence after every fire.

Timezone: DEVICE_LOCAL follows the device timezone. FIXED uses an IANA timezone identifier. Alarms are stored as local wall-clock components, not permanent UTC timestamps.

DST: calendar APIs calculate local wall-clock occurrences. Android reconciles after boot, clock changes, and timezone changes. iOS notification schedules survive reboot and reconcile when the app becomes active.

Recovery: local only. Missing schedules are detected and repaired without backend access.

Stale versions: every fired event carries alarm id and version. The runtime rejects stale versions before Wake Session creation.

Test Alarm: isolated one-shot request. It never mutates the production alarm version or recurrence.

Wake flow: valid fire -> Alarm -> WakeFlowController.begin -> Wake Session -> Mission Engine.
