import {
  BadRequestException,
  Body,
  ConflictException,
  Controller,
  Headers,
  Post,
  UnprocessableEntityException,
} from '@nestjs/common';
import { Type } from 'class-transformer';
import {
  Allow,
  ArrayMaxSize,
  IsArray,
  IsInt,
  IsNotEmpty,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { PoolClient } from 'pg';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

type AlarmSnapshot = Record<string, unknown> & { id: string; version: number; status: string };
type SyncConflict = { id: string; code: string; serverVersion: number | null; serverEntity: AlarmSnapshot | null };

class SyncOperationDto {
  @IsUUID()
  id!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(32)
  operationType!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(32)
  entityType!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(256)
  entityId!: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  clientVersion?: number;

  @IsOptional()
  @IsObject()
  payload?: Record<string, unknown>;

  @Allow()
  occurredAt?: unknown;
}

class SyncBatchDto {
  @IsArray()
  @ArrayMaxSize(100)
  @ValidateNested({ each: true })
  @Type(() => SyncOperationDto)
  operations!: SyncOperationDto[];
}

const ALARM_FIELDS = new Set([
  'label', 'hour', 'minute', 'enabled', 'weekdays', 'timezoneMode', 'fixedTimezone',
  'snoozeEnabled', 'maxSnoozes', 'snoozeMinutes', 'missionType', 'difficulty',
]);
const SELECT_ALARM = `SELECT id,version,label,hour,minute,status='ACTIVE' AS enabled,weekdays,
  timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,
  snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",
  mission_type AS "missionType",difficulty,created_at AS "createdAt",updated_at AS "updatedAt"
  FROM alarms WHERE id=$1 AND anonymous_user_id=$2`;

@Controller('sync')
export class SyncController {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  @Post()
  async sync(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: SyncBatchDto,
  ) {
    const owner = await this.auth.resolve(authorization);
    const response = await this.db.transaction(async (client) => {
      const acceptedIds: string[] = [];
      const conflicts: SyncConflict[] = [];

      for (const operation of body.operations) {
        if (operation.entityType !== 'ALARM') {
          throw new UnprocessableEntityException('UNSUPPORTED_SYNC_ENTITY');
        }
        if (!['UPSERT', 'CREATE_ALARM', 'UPDATE_ALARM', 'DELETE_ALARM'].includes(operation.operationType)) {
          throw new UnprocessableEntityException('UNSUPPORTED_SYNC_OPERATION');
        }
        if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(operation.entityId)) {
          throw new BadRequestException('INVALID_SYNC_ENTITY_ID');
        }

        const occurredAt = this.parseOccurredAt(operation.occurredAt);
        const payload = operation.payload ?? {};
        const normalizedPayload = operation.operationType === 'DELETE_ALARM'
          ? this.normalizePayload({})
          : this.normalizePayload(payload);
        const inserted = await client.query(
          `INSERT INTO sync_operations(id,anonymous_user_id,device_id,operation_type,entity_type,entity_id,client_version,payload,occurred_at,outcome)
           VALUES ($1,$2,(SELECT id FROM devices WHERE anonymous_user_id=$2 ORDER BY updated_at DESC LIMIT 1),$3,$4,$5,$6,$7::jsonb,$8,'PENDING')
           ON CONFLICT (id) DO NOTHING RETURNING id`,
          [operation.id, owner.anonymousUserId, operation.operationType, operation.entityType, operation.entityId,
            operation.clientVersion ?? null, JSON.stringify(payload), occurredAt],
        );

        if (!inserted.rows[0]) {
          const existing = await client.query(
            `SELECT anonymous_user_id AS "anonymousUserId",operation_type AS "operationType",entity_type AS "entityType",
               entity_id AS "entityId",client_version AS "clientVersion",payload=$2::jsonb AS "samePayload",
               occurred_at=$3::timestamptz AS "sameTime",outcome,result
             FROM sync_operations WHERE id=$1`,
            [operation.id, JSON.stringify(payload), occurredAt],
          );
          const row = existing.rows[0];
          if (
            !row || row.anonymousUserId !== owner.anonymousUserId || row.operationType !== operation.operationType ||
            row.entityType !== operation.entityType || row.entityId !== operation.entityId ||
            row.clientVersion !== (operation.clientVersion ?? null) || !row.samePayload || !row.sameTime
          ) throw new ConflictException('SYNC_OPERATION_ID_CONFLICT');
          if (row.outcome === 'CONFLICT') conflicts.push(row.result as SyncConflict);
          else acceptedIds.push(operation.id);
          continue;
        }

        const conflict = await this.applyAlarmOperation(
          client,
          owner.anonymousUserId,
          operation,
          normalizedPayload,
        );
        if (conflict) {
          const storedConflict = { id: operation.id, ...conflict };
          await client.query(
            `UPDATE sync_operations SET outcome='CONFLICT',result=$2::jsonb WHERE id=$1`,
            [operation.id, JSON.stringify(storedConflict)],
          );
          conflicts.push(storedConflict);
        } else {
          await client.query(
            `UPDATE sync_operations SET outcome='APPLIED',result=$2::jsonb WHERE id=$1`,
            [operation.id, JSON.stringify({ applied: true })],
          );
          acceptedIds.push(operation.id);
        }
      }

      return { acceptedIds, conflicts };
    });

    return {
      ...response,
      accepted: response.acceptedIds.length,
      serverTime: new Date().toISOString(),
    };
  }

  private async applyAlarmOperation(
    client: PoolClient,
    ownerId: string,
    operation: SyncOperationDto,
    payload: Record<string, unknown>,
  ): Promise<Omit<SyncConflict, 'id'> | null> {
    await client.query('SELECT pg_advisory_xact_lock(hashtextextended($1,0))', [`${ownerId}:${operation.entityId}`]);
    const existing = await client.query<AlarmSnapshot>(
      `${SELECT_ALARM} FOR UPDATE`,
      [operation.entityId, ownerId],
    );
    const current = existing.rows[0] ?? null;
    const type = operation.operationType;
    const clientVersion = operation.clientVersion ?? null;

    if (type === 'CREATE_ALARM' && current) return this.conflict('ALARM_ALREADY_EXISTS', current);
    if (type === 'UPDATE_ALARM' && !current) return this.conflict('ALARM_NOT_FOUND', null);
    if (type === 'DELETE_ALARM' && !current) return this.conflict('ALARM_NOT_FOUND', null);
    if (current && current.status === 'DELETED' && type !== 'CREATE_ALARM') {
      return this.conflict('ALARM_DELETED', current);
    }
    if (!current && type === 'UPSERT' && clientVersion !== null && clientVersion > 1) {
      return this.conflict('ALARM_NOT_FOUND', null);
    }
    if (current && (clientVersion === null || clientVersion !== current.version)) {
      return this.conflict('VERSION_MISMATCH', current);
    }
    if (type === 'CREATE_ALARM' && clientVersion !== null && clientVersion > 1) {
      return this.conflict('VERSION_MISMATCH', current);
    }

    if (type === 'DELETE_ALARM') {
      const deleted = await client.query<AlarmSnapshot>(
        `UPDATE alarms SET status='DELETED',version=version+1,updated_at=now()
         WHERE id=$1 AND anonymous_user_id=$2
         RETURNING id,version,label,hour,minute,status='ACTIVE' AS enabled,weekdays,
          timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,
          snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",
          mission_type AS "missionType",difficulty,created_at AS "createdAt",updated_at AS "updatedAt"`,
        [operation.entityId, ownerId],
      );
      await this.saveAlarmVersion(client, deleted.rows[0]);
      return null;
    }

    if (!current) {
      const created = await this.insertAlarm(client, ownerId, operation.entityId, payload);
      await this.saveAlarmVersion(client, created);
      return null;
    }

    const merged = { ...this.snapshotDefaults(current), ...payload };
    this.validateAlarm(merged);
    const updated = await client.query<AlarmSnapshot>(
      `UPDATE alarms SET version=version+1,label=$3,hour=$4,minute=$5,weekdays=$6,status=$7,
        timezone_mode=$8,fixed_timezone=$9,snooze_enabled=$10,max_snoozes=$11,snooze_minutes=$12,
        mission_type=$13,difficulty=$14,updated_at=now()
       WHERE id=$1 AND anonymous_user_id=$2
       RETURNING id,version,label,hour,minute,status='ACTIVE' AS enabled,weekdays,
        timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,
        snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",
        mission_type AS "missionType",difficulty,created_at AS "createdAt",updated_at AS "updatedAt"`,
      [operation.entityId, ownerId, merged.label, merged.hour, merged.minute, merged.weekdays,
        merged.enabled ? 'ACTIVE' : 'PAUSED', merged.timezoneMode, merged.fixedTimezone,
        merged.snoozeEnabled, merged.maxSnoozes, merged.snoozeMinutes, merged.missionType, merged.difficulty],
    );
    await this.saveAlarmVersion(client, updated.rows[0]);
    return null;
  }

  private async insertAlarm(
    client: PoolClient,
    ownerId: string,
    id: string,
    payload: Record<string, unknown>,
  ): Promise<AlarmSnapshot> {
    const alarm = this.snapshotDefaults({
      label: 'Alarm', hour: 7, minute: 0, enabled: true, weekdays: [1, 2, 3, 4, 5, 6, 7],
      timezoneMode: 'DEVICE_LOCAL', fixedTimezone: null, snoozeEnabled: true, maxSnoozes: 3,
      snoozeMinutes: 10, missionType: 'MATH', difficulty: 'MEDIUM',
    });
    const merged = { ...alarm, ...payload };
    this.validateAlarm(merged);
    const inserted = await client.query<AlarmSnapshot>(
      `INSERT INTO alarms(id,anonymous_user_id,label,hour,minute,weekdays,status,timezone_mode,fixed_timezone,
        snooze_enabled,max_snoozes,snooze_minutes,mission_type,difficulty)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14)
       RETURNING id,version,label,hour,minute,status='ACTIVE' AS enabled,weekdays,
        timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,
        snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",
        mission_type AS "missionType",difficulty,created_at AS "createdAt",updated_at AS "updatedAt"`,
      [id, ownerId, merged.label, merged.hour, merged.minute, merged.weekdays, merged.enabled ? 'ACTIVE' : 'PAUSED',
        merged.timezoneMode, merged.fixedTimezone, merged.snoozeEnabled, merged.maxSnoozes, merged.snoozeMinutes,
        merged.missionType, merged.difficulty],
    );
    return inserted.rows[0];
  }

  private async saveAlarmVersion(client: PoolClient, alarm: AlarmSnapshot): Promise<void> {
    await client.query(
      'INSERT INTO alarm_versions(alarm_id,version,snapshot) VALUES ($1,$2,$3::jsonb)',
      [alarm.id, alarm.version, JSON.stringify(alarm)],
    );
  }

  private snapshotDefaults(snapshot: Record<string, unknown>): Record<string, unknown> {
    return {
      label: 'Alarm', hour: 7, minute: 0, enabled: true, weekdays: [1, 2, 3, 4, 5, 6, 7],
      timezoneMode: 'DEVICE_LOCAL', fixedTimezone: null, snoozeEnabled: true, maxSnoozes: 3,
      snoozeMinutes: 10, missionType: 'MATH', difficulty: 'MEDIUM', ...snapshot,
    };
  }

  private normalizePayload(payload: Record<string, unknown>): Record<string, unknown> {
    const normalized: Record<string, unknown> = {};
    for (const [key, rawValue] of Object.entries(payload)) {
      if (!ALARM_FIELDS.has(key)) throw new UnprocessableEntityException(`UNSUPPORTED_ALARM_FIELD:${key}`);
      if (key === 'weekdays') {
        let value = rawValue;
        if (typeof value === 'string') {
          try { value = JSON.parse(value); } catch { value = value.split(',').map((part) => Number(part.trim())); }
        }
        if (!Array.isArray(value)) throw new BadRequestException('INVALID_ALARM_WEEKDAYS');
        normalized[key] = value.map((item) => this.numberValue(item, key));
      } else if (['hour', 'minute', 'maxSnoozes', 'snoozeMinutes'].includes(key)) {
        normalized[key] = this.numberValue(rawValue, key);
      } else if (['enabled', 'snoozeEnabled'].includes(key)) {
        normalized[key] = this.booleanValue(rawValue, key);
      } else if (key === 'fixedTimezone') {
        normalized[key] = rawValue === null || rawValue === '' ? null : String(rawValue);
      } else if (typeof rawValue === 'string' || rawValue === null) {
        normalized[key] = rawValue;
      } else {
        throw new BadRequestException(`INVALID_ALARM_FIELD:${key}`);
      }
    }
    return normalized;
  }

  private numberValue(value: unknown, field: string): number {
    const parsed = typeof value === 'number' ? value : typeof value === 'string' && value.trim() ? Number(value) : NaN;
    if (!Number.isInteger(parsed)) throw new BadRequestException(`INVALID_ALARM_FIELD:${field}`);
    return parsed;
  }

  private booleanValue(value: unknown, field: string): boolean {
    if (typeof value === 'boolean') return value;
    if (value === 'true') return true;
    if (value === 'false') return false;
    throw new BadRequestException(`INVALID_ALARM_FIELD:${field}`);
  }

  private validateAlarm(alarm: Record<string, unknown>): void {
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
    if (!['DEVICE_LOCAL', 'FIXED'].includes(String(alarm.timezoneMode))) throw new BadRequestException('INVALID_TIMEZONE_MODE');
    if (alarm.timezoneMode === 'DEVICE_LOCAL') alarm.fixedTimezone = null;
    if (alarm.timezoneMode === 'FIXED' && !this.isValidTimezone(alarm.fixedTimezone)) {
      throw new BadRequestException('INVALID_TIMEZONE');
    }
    if (!['MATH', 'QR', 'STEPS', 'PHOTO', 'MIXED'].includes(String(alarm.missionType))) {
      throw new BadRequestException('INVALID_MISSION_TYPE');
    }
    if (!['EASY', 'MEDIUM', 'HARD'].includes(String(alarm.difficulty))) {
      throw new BadRequestException('INVALID_DIFFICULTY');
    }
  }

  private isValidTimezone(timezone: unknown): boolean {
    if (typeof timezone !== 'string' || !timezone) return false;
    try { new Intl.DateTimeFormat('en-US', { timeZone: timezone }); return true; } catch { return false; }
  }

  private conflict(code: string, alarm: AlarmSnapshot | null): Omit<SyncConflict, 'id'> {
    return { code, serverVersion: alarm?.version ?? null, serverEntity: alarm };
  }

  private parseOccurredAt(value: unknown): Date {
    if (value === undefined || value === null) return new Date();
    let date: Date;
    if (typeof value === 'number' && Number.isFinite(value)) {
      if (value >= 1_000_000_000_000) date = new Date(value);
      else if (value >= 1_000_000_000) date = new Date(value * 1000);
      else date = new Date((value + 978_307_200) * 1000);
    } else if (typeof value === 'string') {
      date = new Date(value);
    } else {
      throw new BadRequestException('INVALID_SYNC_OCCURRED_AT');
    }
    if (!Number.isFinite(date.getTime())) throw new BadRequestException('INVALID_SYNC_OCCURRED_AT');
    return date;
  }
}
