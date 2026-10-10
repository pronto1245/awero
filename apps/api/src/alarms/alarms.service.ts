import {
  BadRequestException,
  NotFoundException,
  Injectable,
} from '@nestjs/common';
import {
  ArrayMaxSize,
  ArrayMinSize,
  ArrayUnique,
  IsArray,
  IsBoolean,
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';
import { ALARM_SNAPSHOT_COLUMNS } from './alarm.repository';
import {
  AlarmDifficulty,
  AlarmMissionType,
  AlarmTimezoneMode,
  defaultAlarmSnapshot,
  validateAlarmSnapshot,
} from './alarm.policy';

export class CreateAlarmDto {
  @IsOptional()
  @IsString()
  @MaxLength(80)
  label?: string;

  @IsInt()
  @Min(0)
  @Max(23)
  hour!: number;

  @IsInt()
  @Min(0)
  @Max(59)
  minute!: number;

  @IsOptional()
  @IsBoolean()
  enabled?: boolean;

  @IsOptional()
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(7)
  @ArrayUnique()
  @IsInt({ each: true })
  @Min(1, { each: true })
  @Max(7, { each: true })
  weekdays?: number[];

  @IsOptional()
  @IsEnum(AlarmTimezoneMode)
  timezoneMode?: AlarmTimezoneMode;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  fixedTimezone?: string;

  @IsOptional()
  @IsBoolean()
  snoozeEnabled?: boolean;

  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(20)
  maxSnoozes?: number;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(60)
  snoozeMinutes?: number;

  @IsOptional()
  @IsEnum(AlarmMissionType)
  missionType?: AlarmMissionType;

  @IsOptional()
  @IsEnum(AlarmDifficulty)
  difficulty?: AlarmDifficulty;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  qrExpectedCode?: string | null;
}

export class UpdateAlarmDto {
  @IsOptional()
  @IsString()
  @MaxLength(80)
  label?: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(23)
  hour?: number;

  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(59)
  minute?: number;

  @IsOptional()
  @IsBoolean()
  enabled?: boolean;

  @IsOptional()
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(7)
  @ArrayUnique()
  @IsInt({ each: true })
  @Min(1, { each: true })
  @Max(7, { each: true })
  weekdays?: number[];

  @IsOptional()
  @IsEnum(AlarmTimezoneMode)
  timezoneMode?: AlarmTimezoneMode;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  fixedTimezone?: string | null;

  @IsOptional()
  @IsBoolean()
  snoozeEnabled?: boolean;

  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(20)
  maxSnoozes?: number;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(60)
  snoozeMinutes?: number;

  @IsOptional()
  @IsEnum(AlarmMissionType)
  missionType?: AlarmMissionType;

  @IsOptional()
  @IsEnum(AlarmDifficulty)
  difficulty?: AlarmDifficulty;

  @IsOptional()
  @IsString()
  @MaxLength(2048)
  qrExpectedCode?: string | null;
}

@Injectable()
export class AlarmService {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  async list(authorization?: string) {
    const owner = await this.auth.resolve(authorization);
    const result = await this.db.query(
      `SELECT ${ALARM_SNAPSHOT_COLUMNS} FROM alarms WHERE anonymous_user_id=$1 AND status<>'DELETED' ORDER BY hour, minute`,
      [owner.anonymousUserId],
    );
    return { items: result.rows };
  }

  async create(authorization: string | undefined, body: CreateAlarmDto) {
    const owner = await this.auth.resolve(authorization);
    const values = Object.fromEntries(Object.entries(body).filter(([, value]) => value !== undefined));
    const alarm = validateAlarmSnapshot(defaultAlarmSnapshot(values));
    const item = await this.db.transaction(async (client) => {
      const inserted = await client.query(
        `INSERT INTO alarms(anonymous_user_id,label,hour,minute,weekdays,status,timezone_mode,fixed_timezone,snooze_enabled,max_snoozes,snooze_minutes,mission_type,difficulty,qr_expected_code) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14) RETURNING ${ALARM_SNAPSHOT_COLUMNS}`,
        [owner.anonymousUserId, alarm.label, alarm.hour, alarm.minute, alarm.weekdays,
          alarm.enabled ? 'ACTIVE' : 'PAUSED', alarm.timezoneMode, alarm.fixedTimezone, alarm.snoozeEnabled,
          alarm.maxSnoozes, alarm.snoozeMinutes, alarm.missionType, alarm.difficulty, alarm.qrExpectedCode],
      );
      const row = inserted.rows[0];
      await client.query(
        'INSERT INTO alarm_versions(alarm_id,version,snapshot) VALUES ($1,$2,$3::jsonb)',
        [row.id, row.version, JSON.stringify(row)],
      );
      return row;
    });
    return { item };
  }

  async update(
    authorization: string | undefined,
    id: string,
    body: UpdateAlarmDto,
  ) {
    const owner = await this.auth.resolve(authorization);
    const changes = Object.fromEntries(
      Object.entries(body).filter(([, value]) => value !== undefined),
    ) as UpdateAlarmDto;
    if (Object.keys(changes).length === 0) throw new BadRequestException('EMPTY_UPDATE');
    const result = await this.db.transaction(async (client) => {
      const current = await client.query(
        `SELECT ${ALARM_SNAPSHOT_COLUMNS} FROM alarms WHERE id=$1 AND anonymous_user_id=$2 AND status<>'DELETED' FOR UPDATE`,
        [id, owner.anonymousUserId],
      );
      if (!current.rows[0]) throw new NotFoundException('ALARM_NOT_FOUND');

      const { enabled, ...alarmChanges } = changes;
      const alarm = validateAlarmSnapshot({
        ...current.rows[0],
        ...alarmChanges,
        status: enabled === undefined ? current.rows[0].status : enabled ? 'ACTIVE' : 'PAUSED',
        enabled: enabled ?? current.rows[0].enabled,
      });

      const updated = await client.query(
        `UPDATE alarms SET version=version+1,label=$3,hour=$4,minute=$5,weekdays=$6,status=$7,timezone_mode=$8,fixed_timezone=$9,snooze_enabled=$10,max_snoozes=$11,snooze_minutes=$12,mission_type=$13,difficulty=$14,qr_expected_code=$15,updated_at=now() WHERE id=$1 AND anonymous_user_id=$2 RETURNING ${ALARM_SNAPSHOT_COLUMNS}`,
        [id, owner.anonymousUserId, alarm.label, alarm.hour, alarm.minute, alarm.weekdays,
          alarm.enabled ? 'ACTIVE' : 'PAUSED', alarm.timezoneMode, alarm.fixedTimezone, alarm.snoozeEnabled,
          alarm.maxSnoozes, alarm.snoozeMinutes, alarm.missionType, alarm.difficulty, alarm.qrExpectedCode ?? null],
      );
      const row = updated.rows[0];
      await client.query(
        'INSERT INTO alarm_versions(alarm_id,version,snapshot) VALUES ($1,$2,$3::jsonb)',
        [row.id, row.version, JSON.stringify(row)],
      );
      return row;
    });
    return { item: result };
  }

  async remove(
    authorization: string | undefined,
    id: string,
  ) {
    const owner = await this.auth.resolve(authorization);
    const deleted = await this.db.transaction(async (client) => {
      const result = await client.query(
        `UPDATE alarms SET status='DELETED',version=version+1,updated_at=now() WHERE id=$1 AND anonymous_user_id=$2 AND status<>'DELETED' RETURNING ${ALARM_SNAPSHOT_COLUMNS}`,
        [id, owner.anonymousUserId],
      );
      if (!result.rows[0]) throw new NotFoundException('ALARM_NOT_FOUND');
      const row = result.rows[0];
      await client.query(
        'INSERT INTO alarm_versions(alarm_id,version,snapshot) VALUES ($1,$2,$3::jsonb)',
        [row.id, row.version, JSON.stringify(row)],
      );
      return row;
    });
    return { deleted: true, id: deleted.id, version: deleted.version };
  }
}
