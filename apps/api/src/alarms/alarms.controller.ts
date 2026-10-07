import { Body, Controller, Delete, Get, Headers, Param, Patch, Post } from '@nestjs/common';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

@Controller('api/v1/alarms')
export class AlarmsController {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  @Get()
  async list(@Headers('authorization') authorization?: string) {
    const owner = await this.auth.resolve(authorization);
    const result = await this.db.query(
      'SELECT id, label, hour, minute, timezone_mode AS "timezoneMode", fixed_timezone AS "fixedTimezone", status, snooze_enabled AS "snoozeEnabled", max_snoozes AS "maxSnoozes", snooze_minutes AS "snoozeMinutes", mission_type AS "missionType", difficulty, created_at AS "createdAt", updated_at AS "updatedAt" FROM alarms WHERE anonymous_user_id=$1 AND status<>\'DELETED\' ORDER BY hour, minute',
      [owner.anonymousUserId],
    );
    return { items: result.rows };
  }

  @Post()
  async create(@Headers('authorization') authorization: string | undefined, @Body() body: any) {
    const owner = await this.auth.resolve(authorization);
    if (!Number.isInteger(body.hour) || body.hour < 0 || body.hour > 23 || !Number.isInteger(body.minute) || body.minute < 0 || body.minute > 59) {
      return { error: 'INVALID_TIME' };
    }
    const result = await this.db.query(
      'INSERT INTO alarms(anonymous_user_id,label,hour,minute,timezone_mode,fixed_timezone,max_snoozes,snooze_minutes,mission_type,difficulty) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) RETURNING *',
      [owner.anonymousUserId, body.label ?? 'Alarm', body.hour, body.minute, body.timezoneMode ?? 'DEVICE_LOCAL', body.fixedTimezone ?? null, body.maxSnoozes ?? 3, body.snoozeMinutes ?? 10, body.missionType ?? 'MATH', body.difficulty ?? 'MEDIUM'],
    );
    return result.rows[0];
  }

  @Patch(':id')
  async update(@Headers('authorization') authorization: string | undefined, @Param('id') id: string, @Body() body: any) {
    const owner = await this.auth.resolve(authorization);
    const result = await this.db.query(
      'UPDATE alarms SET hour=COALESCE($3,hour), minute=COALESCE($4,minute), label=COALESCE($5,label), updated_at=now() WHERE id=$1 AND anonymous_user_id=$2 AND status<>\'DELETED\' RETURNING *',
      [id, owner.anonymousUserId, body.hour ?? null, body.minute ?? null, body.label ?? null],
    );
    if (!result.rows[0]) return { error: 'NOT_FOUND' };
    return result.rows[0];
  }

  @Delete(':id')
  async remove(@Headers('authorization') authorization: string | undefined, @Param('id') id: string) {
    const owner = await this.auth.resolve(authorization);
    const result = await this.db.query(
      'UPDATE alarms SET status=\'DELETED\', updated_at=now() WHERE id=$1 AND anonymous_user_id=$2 RETURNING id',
      [id, owner.anonymousUserId],
    );
    if (!result.rows[0]) return { error: 'NOT_FOUND' };
    return { deleted: true, id };
  }
}
