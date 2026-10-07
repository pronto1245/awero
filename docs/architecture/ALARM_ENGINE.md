# AWERO Alarm Engine v2

## Versioning
Every alarm schedule is identified by:
alarm_id + version + weekday.

Any edit increments version. The previous schedule is cancelled before the new version is installed.

## Recurrence
An alarm stores weekdays 1...7 using Gregorian/Calendar weekday semantics. Each selected day receives its own schedule.

## Timezone
DEVICE_LOCAL follows the current device timezone. FIXED follows the configured IANA timezone identifier.

## DST
The OS calendar/alarm APIs calculate local wall-clock occurrences. AWERO never converts a recurring wall-clock alarm into a fixed UTC timestamp. Recovery runs after timezone/time changes. DST transitions therefore follow local calendar semantics.

## Recovery
On app activation and Android boot/time/timezone broadcasts, AWERO reconciles local alarms with pending OS schedules. Missing schedules are repaired without network access.

## Verification
Verification checks OS pending/pending-intent state. If an enabled alarm is missing, repair recreates it.

## Test Alarm
Test alarms are one-shot and never mutate the production alarm version or recurrence.

## Wake Flow
A fired alarm carries alarm_id and alarm_version. The runtime must reject stale versions before starting a Wake Session.
