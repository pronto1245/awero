import {
  BadRequestException,
  Body,
  ConflictException,
  Controller,
  Get,
  Headers,
  NotFoundException,
  Param,
  ParseUUIDPipe,
  Post,
} from '@nestjs/common';
import { IsDateString, IsEnum, IsInt, IsObject, IsOptional, IsUUID, Max, Min } from 'class-validator';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

enum MissionType {
  MATH = 'MATH',
  QR = 'QR',
  STEPS = 'STEPS',
  PHOTO = 'PHOTO',
  MIXED = 'MIXED',
}

enum WakeEventType {
  AWAKE = 'AWAKE',
  MISSION_STARTED = 'MISSION_STARTED',
  MISSION_VALIDATED = 'MISSION_VALIDATED',
  MISSION_FAILED = 'MISSION_FAILED',
  FALLBACK = 'FALLBACK',
  SNOOZE = 'SNOOZE',
  COMPLETED = 'COMPLETED',
  EMERGENCY_STOP = 'EMERGENCY_STOP',
  CANCELLED = 'CANCELLED',
}

type StoredWakeEventType = WakeEventType | 'TRIGGERED';

class CreateWakeSessionDto {
  @IsUUID()
  id!: string;

  @IsUUID()
  alarmId!: string;

  @IsInt()
  @Min(1)
  alarmVersion!: number;

  @IsDateString()
  scheduledAt!: string;

  @IsDateString()
  triggeredAt!: string;

  @IsEnum(MissionType)
  missionType!: MissionType;

  @IsUUID()
  eventId!: string;
}

class CreateWakeEventDto {
  @IsUUID()
  id!: string;

  @IsEnum(WakeEventType)
  eventType!: WakeEventType;

  @IsOptional()
  @IsDateString()
  occurredAt?: string;

  @IsOptional()
  @IsObject()
  payload?: Record<string, unknown>;
}

@Controller('wake-sessions')
export class WakeSessionsController {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  @Get()
  async list(@Headers('authorization') authorization?: string) {
    const owner = await this.auth.resolve(authorization);
    const result = await this.db.query(
      `SELECT ws.id,ws.alarm_id AS "alarmId",ws.alarm_version AS "alarmVersion",ws.scheduled_at AS "scheduledAt",
        ws.triggered_at AS "triggeredAt",ws.mission_started_at AS "missionStartedAt",ws.completed_at AS "completedAt",
        ws.result,ws.mission_type AS "missionType",ws.completion_time_seconds AS "completionTimeSeconds",
        ws.snooze_count AS "snoozeCount",ws.fallback_used AS "fallbackUsed",ws.emergency_stop AS "emergencyStop"
       FROM wake_sessions ws JOIN alarms a ON a.id=ws.alarm_id
       WHERE a.anonymous_user_id=$1 ORDER BY ws.scheduled_at DESC LIMIT 100`,
      [owner.anonymousUserId],
    );
    return { items: result.rows };
  }

  @Post()
  async create(@Headers('authorization') authorization: string | undefined, @Body() body: CreateWakeSessionDto) {
    const owner = await this.auth.resolve(authorization);
    return this.db.transaction(async (client) => {
      const alarm = await client.query(
        `SELECT a.id FROM alarms a JOIN alarm_versions av ON av.alarm_id=a.id AND av.version=$3
         WHERE a.id=$1 AND a.anonymous_user_id=$2`,
        [body.alarmId, owner.anonymousUserId, body.alarmVersion],
      );
      if (!alarm.rows[0]) throw new NotFoundException('ALARM_VERSION_NOT_FOUND');

      const inserted = await client.query(
        `INSERT INTO wake_sessions(id,alarm_id,alarm_version,scheduled_at,triggered_at,mission_type)
         VALUES ($1,$2,$3,$4,$5,$6) ON CONFLICT (id) DO NOTHING RETURNING id`,
        [body.id, body.alarmId, body.alarmVersion, body.scheduledAt, body.triggeredAt, body.missionType],
      );
      if (!inserted.rows[0]) {
        const existing = await client.query(
          `SELECT ws.id,ws.scheduled_at=$5::timestamptz AS "sameScheduledAt",
             ws.triggered_at=$6::timestamptz AS "sameTriggeredAt",ws.mission_type=$7 AS "sameMissionType"
           FROM wake_sessions ws JOIN alarms a ON a.id=ws.alarm_id
           WHERE ws.id=$1 AND ws.alarm_id=$2 AND ws.alarm_version=$3 AND a.anonymous_user_id=$4`,
          [body.id, body.alarmId, body.alarmVersion, owner.anonymousUserId,
            body.scheduledAt, body.triggeredAt, body.missionType],
        );
        if (
          !existing.rows[0] || !existing.rows[0].sameScheduledAt || !existing.rows[0].sameTriggeredAt ||
          !existing.rows[0].sameMissionType
        ) throw new ConflictException('WAKE_SESSION_ID_CONFLICT');
        const triggerEvent = await client.query(
          `SELECT id FROM wake_events WHERE wake_session_id=$1 AND event_type='TRIGGERED'`,
          [body.id],
        );
        if (triggerEvent.rows[0]?.id !== body.eventId) throw new ConflictException('WAKE_SESSION_EVENT_CONFLICT');
      } else {
        const event = await client.query(
          `INSERT INTO wake_events(id,wake_session_id,event_type,occurred_at,payload)
           VALUES ($1,$2,'TRIGGERED',$3,'{}'::jsonb) ON CONFLICT (id) DO NOTHING RETURNING id`,
          [body.eventId, body.id, body.triggeredAt],
        );
        if (!event.rows[0]) throw new ConflictException('WAKE_EVENT_ID_CONFLICT');
      }
      const session = await client.query(
        `SELECT id,alarm_id AS "alarmId",alarm_version AS "alarmVersion",scheduled_at AS "scheduledAt",
          triggered_at AS "triggeredAt",mission_started_at AS "missionStartedAt",completed_at AS "completedAt",
          result,mission_type AS "missionType",completion_time_seconds AS "completionTimeSeconds",
          snooze_count AS "snoozeCount",fallback_used AS "fallbackUsed",emergency_stop AS "emergencyStop"
         FROM wake_sessions WHERE id=$1`,
        [body.id],
      );
      return { item: session.rows[0], duplicate: inserted.rowCount === 0 };
    });
  }

  @Post(':id/events')
  async appendEvent(
    @Headers('authorization') authorization: string | undefined,
    @Param('id', new ParseUUIDPipe()) id: string,
    @Body() body: CreateWakeEventDto,
  ) {
    const owner = await this.auth.resolve(authorization);
    const item = await this.db.transaction(async (client) => {
      const current = await client.query(
        `SELECT ws.id,ws.triggered_at AS "triggeredAt",ws.mission_started_at AS "missionStartedAt",
          ws.completed_at AS "completedAt",ws.result,ws.snooze_count AS "snoozeCount",
          ws.fallback_used AS "fallbackUsed",ws.emergency_stop AS "emergencyStop"
         FROM wake_sessions ws JOIN alarms a ON a.id=ws.alarm_id
         WHERE ws.id=$1 AND a.anonymous_user_id=$2 FOR UPDATE OF ws`,
        [id, owner.anonymousUserId],
      );
      const session = current.rows[0];
      if (!session) throw new NotFoundException('WAKE_SESSION_NOT_FOUND');

      const latest = await client.query(
        `SELECT event_type AS "eventType" FROM wake_events
         WHERE wake_session_id=$1 AND event_type<>'SNOOZE' ORDER BY occurred_at DESC,id DESC LIMIT 1`,
        [id],
      );
      const lastStateEvent = latest.rows[0]?.eventType as StoredWakeEventType | undefined;

      const inserted = await client.query(
        `INSERT INTO wake_events(id,wake_session_id,event_type,occurred_at,payload)
         VALUES ($1,$2,$3,COALESCE($4::timestamptz,now()),$5::jsonb) ON CONFLICT (id) DO NOTHING RETURNING id`,
        [body.id, id, body.eventType, body.occurredAt ?? null, body.payload ?? {}],
      );
      if (!inserted.rows[0]) {
        const existing = await client.query(
          `SELECT wake_session_id AS "wakeSessionId",event_type AS "eventType",payload=$2::jsonb AS "samePayload",
             ($3::timestamptz IS NULL OR occurred_at=$3::timestamptz) AS "sameOccurredAt"
           FROM wake_events WHERE id=$1`,
          [body.id, JSON.stringify(body.payload ?? {}), body.occurredAt ?? null],
        );
        if (
          existing.rows[0]?.wakeSessionId !== id ||
          existing.rows[0]?.eventType !== body.eventType ||
          !existing.rows[0]?.samePayload || !existing.rows[0]?.sameOccurredAt
        ) throw new ConflictException('WAKE_EVENT_ID_CONFLICT');
        return { id, duplicate: true };
      }
      if (session.result !== null) throw new ConflictException('WAKE_SESSION_ALREADY_FINISHED');

      const at = body.occurredAt ?? new Date().toISOString();
      switch (body.eventType) {
        case WakeEventType.AWAKE:
          if (!session.triggeredAt) throw new ConflictException('WAKE_SESSION_NOT_TRIGGERED');
          break;
        case WakeEventType.MISSION_STARTED:
          if (
            !session.triggeredAt ||
            (lastStateEvent !== WakeEventType.AWAKE && lastStateEvent !== 'TRIGGERED' && lastStateEvent !== WakeEventType.FALLBACK)
          ) {
            throw new ConflictException('INVALID_MISSION_START');
          }
          await client.query('UPDATE wake_sessions SET mission_started_at=COALESCE(mission_started_at,$2) WHERE id=$1', [id, at]);
          break;
        case WakeEventType.MISSION_VALIDATED:
        case WakeEventType.MISSION_FAILED:
          if (!session.missionStartedAt || lastStateEvent !== WakeEventType.MISSION_STARTED) {
            throw new ConflictException('MISSION_NOT_STARTED');
          }
          break;
        case WakeEventType.FALLBACK:
          if (!session.missionStartedAt || session.fallbackUsed || lastStateEvent !== WakeEventType.MISSION_FAILED) {
            throw new ConflictException('INVALID_FALLBACK');
          }
          await client.query('UPDATE wake_sessions SET fallback_used=true WHERE id=$1', [id]);
          break;
        case WakeEventType.SNOOZE: {
          const count = body.payload?.count;
          if (!Number.isInteger(count) || (count as number) < 1 || (count as number) > 20) {
            throw new BadRequestException('INVALID_SNOOZE_COUNT');
          }
          if ((count as number) <= session.snoozeCount) throw new ConflictException('SNOOZE_COUNT_NOT_INCREASING');
          await client.query('UPDATE wake_sessions SET snooze_count=$2 WHERE id=$1', [id, count]);
          break;
        }
        case WakeEventType.COMPLETED:
          if (
            !session.missionStartedAt ||
            (lastStateEvent !== WakeEventType.MISSION_STARTED &&
              lastStateEvent !== WakeEventType.MISSION_VALIDATED &&
              lastStateEvent !== WakeEventType.FALLBACK)
          ) {
            throw new ConflictException('MISSION_NOT_STARTED');
          }
          await client.query(
            `UPDATE wake_sessions SET completed_at=$2,result='COMPLETED',completion_time_seconds=GREATEST(0,FLOOR(EXTRACT(EPOCH FROM ($2::timestamptz-triggered_at)))::integer) WHERE id=$1`,
            [id, at],
          );
          break;
        case WakeEventType.EMERGENCY_STOP:
          await client.query(`UPDATE wake_sessions SET completed_at=$2,result='EMERGENCY_STOP',emergency_stop=true WHERE id=$1`, [id, at]);
          break;
        case WakeEventType.CANCELLED:
          await client.query(`UPDATE wake_sessions SET completed_at=$2,result='CANCELLED' WHERE id=$1`, [id, at]);
          break;
      }
      return { id, duplicate: false };
    });
    return { item };
  }

  @Get(':id/events')
  async events(
    @Headers('authorization') authorization: string | undefined,
    @Param('id', new ParseUUIDPipe()) id: string,
  ) {
    const owner = await this.auth.resolve(authorization);
    const session = await this.db.query(
      'SELECT ws.id FROM wake_sessions ws JOIN alarms a ON a.id=ws.alarm_id WHERE ws.id=$1 AND a.anonymous_user_id=$2',
      [id, owner.anonymousUserId],
    );
    if (!session.rows[0]) throw new NotFoundException('WAKE_SESSION_NOT_FOUND');
    const events = await this.db.query(
      `SELECT id,wake_session_id AS "wakeSessionId",event_type AS "eventType",occurred_at AS "occurredAt",payload
       FROM wake_events WHERE wake_session_id=$1 ORDER BY occurred_at,id`,
      [id],
    );
    return { items: events.rows };
  }
}
