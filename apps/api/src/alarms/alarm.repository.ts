/** Canonical API snapshot shared by CRUD, push conflicts, and inbound reconciliation. */
export const ALARM_SNAPSHOT_COLUMNS = `id,version,label,hour,minute,status='ACTIVE' AS enabled,weekdays,
  timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,
  snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",
  mission_type AS "missionType",difficulty,qr_expected_code AS "qrExpectedCode",
  created_at AS "createdAt",updated_at AS "updatedAt"`;
