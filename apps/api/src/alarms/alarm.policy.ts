import { BadRequestException, UnprocessableEntityException } from '@nestjs/common';

export enum AlarmTimezoneMode {
  DEVICE_LOCAL = 'DEVICE_LOCAL',
  FIXED = 'FIXED',
}

export enum AlarmMissionType {
  MATH = 'MATH',
  QR = 'QR',
  STEPS = 'STEPS',
  PHOTO = 'PHOTO',
  MIXED = 'MIXED',
}

export enum AlarmDifficulty {
  EASY = 'EASY',
  MEDIUM = 'MEDIUM',
  HARD = 'HARD',
}

const ALARM_FIELDS = new Set([
  'label', 'hour', 'minute', 'enabled', 'weekdays', 'timezoneMode', 'fixedTimezone',
  'snoozeEnabled', 'maxSnoozes', 'snoozeMinutes', 'missionType', 'difficulty', 'qrExpectedCode',
]);

export function defaultAlarmSnapshot(values: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    label: 'Alarm', hour: 7, minute: 0, enabled: true, weekdays: [1, 2, 3, 4, 5, 6, 7],
    timezoneMode: AlarmTimezoneMode.DEVICE_LOCAL, fixedTimezone: null, snoozeEnabled: true,
    maxSnoozes: 3, snoozeMinutes: 10, missionType: AlarmMissionType.MATH,
    difficulty: AlarmDifficulty.MEDIUM, qrExpectedCode: null, ...values,
  };
}

export function normalizeAlarmPayload(payload: Record<string, unknown>): Record<string, unknown> {
  const normalized: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(payload)) {
    if (!ALARM_FIELDS.has(key)) throw new UnprocessableEntityException(`UNSUPPORTED_ALARM_FIELD:${key}`);
    if (key === 'weekdays') {
      let days = value;
      if (typeof days === 'string') {
        const encodedDays = days;
        try { days = JSON.parse(encodedDays); } catch {
          days = encodedDays.split(',').map((part: string) => Number(part.trim()));
        }
      }
      if (!Array.isArray(days)) throw new BadRequestException('INVALID_ALARM_WEEKDAYS');
      normalized[key] = days.map((item) => numberValue(item, key));
    } else if (['hour', 'minute', 'maxSnoozes', 'snoozeMinutes'].includes(key)) {
      normalized[key] = numberValue(value, key);
    } else if (['enabled', 'snoozeEnabled'].includes(key)) {
      normalized[key] = booleanValue(value, key);
    } else if (key === 'fixedTimezone') {
      normalized[key] = value === null || value === '' ? null : String(value);
    } else if (key === 'qrExpectedCode') {
      if (value !== null && (typeof value !== 'string' || value.length > 2048)) {
        throw new BadRequestException('INVALID_ALARM_FIELD:qrExpectedCode');
      }
      normalized[key] = value;
    } else if (typeof value === 'string' || value === null) {
      normalized[key] = value;
    } else {
      throw new BadRequestException(`INVALID_ALARM_FIELD:${key}`);
    }
  }
  return normalized;
}

export function validateAlarmSnapshot(input: Record<string, unknown>): Record<string, unknown> {
  const alarm = { ...input };
  const integerInRange = (key: string, min: number, max: number) =>
    Number.isInteger(alarm[key]) && Number(alarm[key]) >= min && Number(alarm[key]) <= max;
  if (typeof alarm.label !== 'string' || alarm.label.length > 80) throw new BadRequestException('INVALID_ALARM_LABEL');
  if (!integerInRange('hour', 0, 23)) throw new BadRequestException('INVALID_ALARM_HOUR');
  if (!integerInRange('minute', 0, 59)) throw new BadRequestException('INVALID_ALARM_MINUTE');
  if (!Array.isArray(alarm.weekdays) || alarm.weekdays.length < 1 || alarm.weekdays.length > 7 ||
    alarm.weekdays.some((day) => !Number.isInteger(day) || day < 1 || day > 7) ||
    new Set(alarm.weekdays).size !== alarm.weekdays.length) throw new BadRequestException('INVALID_ALARM_WEEKDAYS');
  if (typeof alarm.enabled !== 'boolean' || typeof alarm.snoozeEnabled !== 'boolean') {
    throw new BadRequestException('INVALID_ALARM_BOOLEAN');
  }
  if (!integerInRange('maxSnoozes', 0, 20)) throw new BadRequestException('INVALID_ALARM_MAX_SNOOZES');
  if (!integerInRange('snoozeMinutes', 1, 60)) throw new BadRequestException('INVALID_ALARM_SNOOZE_MINUTES');
  if (!Object.values(AlarmTimezoneMode).includes(alarm.timezoneMode as AlarmTimezoneMode)) {
    throw new BadRequestException('INVALID_TIMEZONE_MODE');
  }
  if (alarm.timezoneMode === AlarmTimezoneMode.DEVICE_LOCAL) alarm.fixedTimezone = null;
  if (alarm.timezoneMode === AlarmTimezoneMode.FIXED && !isValidTimezone(alarm.fixedTimezone)) {
    throw new BadRequestException('INVALID_TIMEZONE');
  }
  if (!Object.values(AlarmMissionType).includes(alarm.missionType as AlarmMissionType)) {
    throw new BadRequestException('INVALID_MISSION_TYPE');
  }
  if (!Object.values(AlarmDifficulty).includes(alarm.difficulty as AlarmDifficulty)) {
    throw new BadRequestException('INVALID_DIFFICULTY');
  }
  if (alarm.qrExpectedCode !== null && alarm.qrExpectedCode !== undefined &&
    (typeof alarm.qrExpectedCode !== 'string' || alarm.qrExpectedCode.length > 2048)) {
    throw new BadRequestException('INVALID_ALARM_FIELD:qrExpectedCode');
  }
  return alarm;
}

function numberValue(value: unknown, field: string): number {
  const parsed = typeof value === 'number' ? value : typeof value === 'string' && value.trim() ? Number(value) : NaN;
  if (!Number.isInteger(parsed)) throw new BadRequestException(`INVALID_ALARM_FIELD:${field}`);
  return parsed;
}

function booleanValue(value: unknown, field: string): boolean {
  if (typeof value === 'boolean') return value;
  if (value === 'true') return true;
  if (value === 'false') return false;
  throw new BadRequestException(`INVALID_ALARM_FIELD:${field}`);
}

function isValidTimezone(value: unknown): boolean {
  if (typeof value !== 'string' || !value) return false;
  try { new Intl.DateTimeFormat('en-US', { timeZone: value }); return true; } catch { return false; }
}
