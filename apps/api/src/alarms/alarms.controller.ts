import {
  BadRequestException,
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  NotFoundException,
  Param,
  ParseUUIDPipe,
  Patch,
  Post,
} from '@nestjs/common';
import { IsBoolean, IsEnum, IsInt, IsOptional, IsString, Max, MaxLength, Min } from 'class-validator';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

enum AlarmTimezoneMode {
  DEVICE_LOCAL = 'DEVICE_LOCAL',
  FIXED = 'FIXED',
}

enum AlarmMissionType {
  MATH = 'MATH',
  QR = 'QR',
  STEPS = 'STEPS',
  PHOTO = 'PHOTO',
  MIXED = 'MIXED',
}

enum AlarmDifficulty {
  EASY = 'EASY',
  MEDIUM = 'MEDIUM',
  HARD = 'HARD',
}

class CreateAlarmDto {
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
}

class UpdateAlarmDto {
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
}

@Controller('alarms')
export class AlarmsController {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  @Get()
  async list(@Headers('authorization') authorization?: string) {
    const owner = await this.auth.resolve(authorization);
    const result = await this.db.query(
      'SELECT id, version, label, hour, minute, timezone_mode AS "timezoneMode", fixed_timezone AS "fixedTimezone", status, snooze_enabled AS "snoozeEnabled", max_snoozes AS "maxSnoozes", snooze_minutes AS "snoozeMinutes", mission_type AS "missionType", difficulty, created_at AS "createdAt", updated_at AS "updatedAt" FROM alarms WHERE anonymous_user_id=$1 AND status<>\'DELETED\' ORDER BY hour, minute',
      [owner.anonymousUserId],
    );
    return { items: result.rows };
  }

  @Post()
  async create(@Headers('authorization') authorization: string | undefined, @Body() body: CreateAlarmDto) {
    const owner = await this.auth.resolve(authorization);
    const timezoneMode = body.timezoneMode ?? AlarmTimezoneMode.DEVICE_LOCAL;
    const fixedTimezone = body.fixedTimezone ?? null;
    if (timezoneMode === AlarmTimezoneMode.FIXED && !this.isValidTimezone(fixedTimezone)) {
      throw new BadRequestException('INVALID_TIMEZONE');
    }
    if (timezoneMode === AlarmTimezoneMode.DEVICE_LOCAL && fixedTimezone !== null) {
      throw new BadRequestException('FIXED_TIMEZONE_REQUIRES_FIXED_MODE');
    }
    const item = await this.db.transaction(async (client) => {
      const inserted = await client.query(
        'INSERT INTO alarms(anonymous_user_id,label,hour,minute,timezone_mode,fixed_timezone,snooze_enabled,max_snoozes,snooze_minutes,mission_type,difficulty) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11) RETURNING id,version,label,hour,minute,timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",mission_type AS "missionType",difficulty,created_at AS "createdAt",updated_at AS "updatedAt"',
        [owner.anonymousUserId, body.label ?? 'Alarm', body.hour, body.minute, timezoneMode, fixedTimezone, body.snoozeEnabled ?? true, body.maxSnoozes ?? 3, body.snoozeMinutes ?? 10, body.missionType ?? AlarmMissionType.MATH, body.difficulty ?? AlarmDifficulty.MEDIUM],
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

  @Patch(':id')
  async update(
    @Headers('authorization') authorization: string | undefined,
    @Param('id', new ParseUUIDPipe()) id: string,
    @Body() body: UpdateAlarmDto,
  ) {
    const owner = await this.auth.resolve(authorization);
    if (Object.keys(body).length === 0) throw new BadRequestException('EMPTY_UPDATE');
    const result = await this.db.transaction(async (client) => {
      const current = await client.query(
        'SELECT id,version,label,hour,minute,timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",mission_type AS "missionType",difficulty FROM alarms WHERE id=$1 AND anonymous_user_id=$2 AND status<>\'DELETED\' FOR UPDATE',
        [id, owner.anonymousUserId],
      );
      if (!current.rows[0]) throw new NotFoundException('ALARM_NOT_FOUND');

      const alarm = { ...current.rows[0], ...body };
      if (alarm.timezoneMode === AlarmTimezoneMode.FIXED && !this.isValidTimezone(alarm.fixedTimezone)) {
        throw new BadRequestException('INVALID_TIMEZONE');
      }
      if (alarm.timezoneMode === AlarmTimezoneMode.DEVICE_LOCAL) alarm.fixedTimezone = null;

      const updated = await client.query(
        'UPDATE alarms SET version=version+1,label=$3,hour=$4,minute=$5,timezone_mode=$6,fixed_timezone=$7,snooze_enabled=$8,max_snoozes=$9,snooze_minutes=$10,mission_type=$11,difficulty=$12,updated_at=now() WHERE id=$1 AND anonymous_user_id=$2 RETURNING id,version,label,hour,minute,timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",mission_type AS "missionType",difficulty,created_at AS "createdAt",updated_at AS "updatedAt"',
        [id, owner.anonymousUserId, alarm.label, alarm.hour, alarm.minute, alarm.timezoneMode, alarm.fixedTimezone, alarm.snoozeEnabled, alarm.maxSnoozes, alarm.snoozeMinutes, alarm.missionType, alarm.difficulty],
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

  @Delete(':id')
  async remove(
    @Headers('authorization') authorization: string | undefined,
    @Param('id', new ParseUUIDPipe()) id: string,
  ) {
    const owner = await this.auth.resolve(authorization);
    const deleted = await this.db.transaction(async (client) => {
      const result = await client.query(
        'UPDATE alarms SET status=\'DELETED\',version=version+1,updated_at=now() WHERE id=$1 AND anonymous_user_id=$2 AND status<>\'DELETED\' RETURNING id,version,label,hour,minute,timezone_mode AS "timezoneMode",fixed_timezone AS "fixedTimezone",status,snooze_enabled AS "snoozeEnabled",max_snoozes AS "maxSnoozes",snooze_minutes AS "snoozeMinutes",mission_type AS "missionType",difficulty,created_at AS "createdAt",updated_at AS "updatedAt"',
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

  private isValidTimezone(timezone: string | null | undefined): boolean {
    if (!timezone) return false;
    try {
      new Intl.DateTimeFormat('en-US', { timeZone: timezone });
      return true;
    } catch {
      return false;
    }
  }
}
