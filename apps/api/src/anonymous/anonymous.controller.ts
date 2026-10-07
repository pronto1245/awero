import { Body, Controller, Post } from '@nestjs/common';
import { DatabaseService } from '../database/database.service';

class RegisterAnonymousDto {
  deviceId!: string;
  platform!: string;
  appVersion!: string;
  osVersion?: string;
  timezone!: string;
}

@Controller('api/v1/auth')
export class AnonymousController {
  constructor(private readonly db: DatabaseService) {}

  @Post('anonymous')
  async register(@Body() body: RegisterAnonymousDto) {
    if (!body.deviceId || !body.platform || !body.appVersion || !body.timezone) {
      return { error: 'INVALID_REQUEST' };
    }

    const result = await this.db.query(
      'INSERT INTO anonymous_users(device_id) VALUES ($1) ON CONFLICT(device_id) DO UPDATE SET device_id=EXCLUDED.device_id RETURNING id',
      [body.deviceId],
    );

    const anonymousUserId = result.rows[0].id;
    await this.db.query(
      'INSERT INTO devices(anonymous_user_id, platform, app_version, os_version, timezone) VALUES ($1,$2,$3,$4,$5)',
      [anonymousUserId, body.platform, body.appVersion, body.osVersion ?? null, body.timezone],
    );

    return { anonymousUserId, authMode: 'anonymous', syncEnabled: true };
  }
}
