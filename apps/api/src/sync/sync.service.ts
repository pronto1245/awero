import {
  BadRequestException,
  ConflictException,
  HttpException,
  UnprocessableEntityException,
  Injectable,
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
import { ALARM_SNAPSHOT_COLUMNS } from '../alarms/alarm.repository';
import { defaultAlarmSnapshot, normalizeAlarmPayload, validateAlarmSnapshot } from '../alarms/alarm.policy';

type AlarmSnapshot = Record<string, unknown> & { id: string; version: number; status: string };
type SyncConflict = { id: string; code: string; serverVersion: number | null; serverEntity: AlarmSnapshot | null };
type SyncRejection = { id: string; code: string };

export class SyncOperationDto {
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

export class SyncBatchDto {
  @IsArray()
  @ArrayMaxSize(100)
  @ValidateNested({ each: true })
  @Type(() => SyncOperationDto)
  operations!: SyncOperationDto[];
}

const SELECT_ALARM = `SELECT ${ALARM_SNAPSHOT_COLUMNS} FROM alarms WHERE id=$1 AND anonymous_user_id=$2`;
const SELECT_OWNER_ALARMS = `SELECT ${ALARM_SNAPSHOT_COLUMNS} FROM alarms WHERE anonymous_user_id=$1 ORDER BY updated_at,id`;

@Injectable()
export class SyncService {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  async serverAlarms(authorization?: string) {
    const owner = await this.auth.resolve(authorization);
    const result = await this.db.query(SELECT_OWNER_ALARMS, [owner.anonymousUserId]);
    return { items: result.rows, serverTime: new Date().toISOString() };
  }

  async sync(
    authorization: string | undefined,
    body: SyncBatchDto,
  ) {
    const owner = await this.auth.resolve(authorization);
    const response = await this.db.transaction(async (client) => {
      const acceptedIds: string[] = [];
      const conflicts: SyncConflict[] = [];
      const rejected: SyncRejection[] = [];

      for (const operation of body.operations) {
        const savepoint = `sync_op_${acceptedIds.length}_${conflicts.length}_${rejected.length}`;
        await client.query(`SAVEPOINT ${savepoint}`);
        try {
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
        const retryOccurredAt = operation.occurredAt === undefined || operation.occurredAt === null
          ? null
          : occurredAt;
        const payload = operation.payload ?? {};
        const normalizedPayload = operation.operationType === 'DELETE_ALARM'
          ? normalizeAlarmPayload({})
          : normalizeAlarmPayload(payload);
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
               ($3::timestamptz IS NULL OR occurred_at=$3::timestamptz) AS "sameTime",outcome,result
             FROM sync_operations WHERE id=$1`,
            [operation.id, JSON.stringify(payload), retryOccurredAt],
          );
          const row = existing.rows[0];
          if (
            !row || row.anonymousUserId !== owner.anonymousUserId || row.operationType !== operation.operationType ||
            row.entityType !== operation.entityType || row.entityId !== operation.entityId ||
            row.clientVersion !== (operation.clientVersion ?? null) || !row.samePayload || !row.sameTime
          ) throw new ConflictException('SYNC_OPERATION_ID_CONFLICT');
          if (row.outcome === 'CONFLICT') conflicts.push(row.result as SyncConflict);
          else if (row.outcome === 'REJECTED') rejected.push(row.result as SyncRejection);
          else acceptedIds.push(operation.id);
          await client.query(`RELEASE SAVEPOINT ${savepoint}`);
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
        await client.query(`RELEASE SAVEPOINT ${savepoint}`);
        } catch (error) {
          const rejectionCode = this.rejectionCode(error);
          if (!rejectionCode) throw error;
          await client.query(`ROLLBACK TO SAVEPOINT ${savepoint}`);
          const payload = operation.payload ?? {};
          const occurredAt = this.safeOccurredAt(operation.occurredAt);
          const result = { id: operation.id, code: rejectionCode };
          await client.query(
            `INSERT INTO sync_operations(id,anonymous_user_id,device_id,operation_type,entity_type,entity_id,
               client_version,payload,occurred_at,outcome,result)
             VALUES ($1,$2,(SELECT id FROM devices WHERE anonymous_user_id=$2 ORDER BY updated_at DESC LIMIT 1),
               $3,$4,$5,$6,$7::jsonb,$8,'REJECTED',$9::jsonb)
             ON CONFLICT (id) DO NOTHING`,
            [operation.id, owner.anonymousUserId, operation.operationType, operation.entityType, operation.entityId,
              operation.clientVersion ?? null, JSON.stringify(payload), occurredAt, JSON.stringify(result)],
          );
          rejected.push(result);
          await client.query(`RELEASE SAVEPOINT ${savepoint}`);
        }
      }

      return { acceptedIds, conflicts, rejected };
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
         RETURNING ${ALARM_SNAPSHOT_COLUMNS}`,
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

    const merged = validateAlarmSnapshot({ ...defaultAlarmSnapshot(current), ...payload });
    const updated = await client.query<AlarmSnapshot>(
      `UPDATE alarms SET version=version+1,label=$3,hour=$4,minute=$5,weekdays=$6,status=$7,
        timezone_mode=$8,fixed_timezone=$9,snooze_enabled=$10,max_snoozes=$11,snooze_minutes=$12,
       mission_type=$13,difficulty=$14,qr_expected_code=$15,updated_at=now()
       WHERE id=$1 AND anonymous_user_id=$2
       RETURNING ${ALARM_SNAPSHOT_COLUMNS}`,
      [operation.entityId, ownerId, merged.label, merged.hour, merged.minute, merged.weekdays,
        merged.enabled ? 'ACTIVE' : 'PAUSED', merged.timezoneMode, merged.fixedTimezone,
        merged.snoozeEnabled, merged.maxSnoozes, merged.snoozeMinutes, merged.missionType, merged.difficulty,
        merged.qrExpectedCode ?? null],
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
    const alarm = validateAlarmSnapshot(defaultAlarmSnapshot({
      label: 'Alarm', hour: 7, minute: 0, enabled: true, weekdays: [1, 2, 3, 4, 5, 6, 7],
      timezoneMode: 'DEVICE_LOCAL', fixedTimezone: null, snoozeEnabled: true, maxSnoozes: 3,
      snoozeMinutes: 10, missionType: 'MATH', difficulty: 'MEDIUM',
    }));
    const merged = validateAlarmSnapshot({ ...alarm, ...payload });
    const inserted = await client.query<AlarmSnapshot>(
      `INSERT INTO alarms(id,anonymous_user_id,label,hour,minute,weekdays,status,timezone_mode,fixed_timezone,
       snooze_enabled,max_snoozes,snooze_minutes,mission_type,difficulty,qr_expected_code)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15)
       RETURNING ${ALARM_SNAPSHOT_COLUMNS}`,
      [id, ownerId, merged.label, merged.hour, merged.minute, merged.weekdays, merged.enabled ? 'ACTIVE' : 'PAUSED',
        merged.timezoneMode, merged.fixedTimezone, merged.snoozeEnabled, merged.maxSnoozes, merged.snoozeMinutes,
        merged.missionType, merged.difficulty, merged.qrExpectedCode ?? null],
    );
    return inserted.rows[0];
  }

  private async saveAlarmVersion(client: PoolClient, alarm: AlarmSnapshot): Promise<void> {
    await client.query(
      'INSERT INTO alarm_versions(alarm_id,version,snapshot) VALUES ($1,$2,$3::jsonb)',
      [alarm.id, alarm.version, JSON.stringify(alarm)],
    );
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
    const now = Date.now();
    if (date.getTime() < now - 30 * 24 * 60 * 60 * 1000 || date.getTime() > now + 5 * 60 * 1000) {
      throw new BadRequestException('SYNC_OCCURRED_AT_OUT_OF_RANGE');
    }
    return date;
  }

  private safeOccurredAt(value: unknown): Date {
    try { return this.parseOccurredAt(value); } catch { return new Date(); }
  }

  private rejectionCode(error: unknown): string | null {
    if (error instanceof HttpException && error.getStatus() >= 400 && error.getStatus() < 500) {
      const response = error.getResponse();
      if (typeof response === 'string') return response;
      if (response && typeof response === 'object' && 'message' in response) {
        const message = (response as { message: unknown }).message;
        return typeof message === 'string' ? message : 'INVALID_SYNC_OPERATION';
      }
      return 'INVALID_SYNC_OPERATION';
    }
    const code = (error as { code?: string; constraint?: string })?.code;
    if (code === '23505' && (error as { constraint?: string }).constraint === 'alarms_pkey') {
      return 'ALARM_ID_OWNERSHIP_CONFLICT';
    }
    if (['23503', '23514', '22P02'].includes(code ?? '')) return 'INVALID_SYNC_OPERATION';
    return null;
  }
}
