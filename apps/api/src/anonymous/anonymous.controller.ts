import { Body, Controller, Post } from '@nestjs/common';
import { createHash, randomBytes } from 'crypto';
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

    const user = await this.db.query(
      'INSERT INTO anonymous_users(device_id) VALUES ($1) ON CONFLICT(device_id) DO UPDATE SET device_id=EXCLUDED.device_id RETURNING id',
      [body.deviceId],
    );
    const anonymousUserId = user.rows[0].id;

    const device = await this.db.query(
      'INSERT INTO devices(anonymous_user_id, platform, app_version, os_version, timezone) VALUES ($1,$2,$3,$4,$5) RETURNING id',
      [anonymousUserId, body.platform, body.appVersion, body.osVersion ?? null, body.timezone],
    );

    const rawToken = randomBytes(32).toString('base64url');
    const tokenHash = createHash('sha256').update(rawToken).digest('hex');

    await this.db.query(
      'INSERT INTO auth_sessions(anonymous_user_id, token_hash, expires_at) VALUES ($1,$2,now() + interval \'30 days\')',
      [anonymousUserId, tokenHash],
    );

    return {
      anonymousUserId,
      deviceId: device.rows[0].id,
      accessToken: rawToken,
      tokenType: 'Bearer',
      expiresInDays: 30,
      authMode: 'anonymous',
      syncEnabled: true,
    };
  }
}
