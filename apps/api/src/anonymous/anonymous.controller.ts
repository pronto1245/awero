import { BadRequestException, Body, Controller, Post } from '@nestjs/common';
import { createHash, randomBytes } from 'crypto';
import { IsIn, IsOptional, IsString, Length, MaxLength } from 'class-validator';
import { DatabaseService } from '../database/database.service';

class RegisterAnonymousDto {
  @IsString()
  @Length(16, 128)
  deviceId!: string;

  @IsIn(['IOS', 'ANDROID'])
  platform!: string;

  @IsString()
  @Length(1, 64)
  appVersion!: string;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  osVersion?: string;

  @IsString()
  @Length(1, 64)
  timezone!: string;
}

@Controller('auth')
export class AnonymousController {
  constructor(private readonly db: DatabaseService) {}

  @Post('anonymous')
  async register(@Body() body: RegisterAnonymousDto) {
    try {
      new Intl.DateTimeFormat('en-US', { timeZone: body.timezone });
    } catch {
      throw new BadRequestException('INVALID_REQUEST');
    }

    return this.db.transaction(async (client) => {
      const user = await client.query(
        'INSERT INTO anonymous_users(device_id) VALUES ($1) ON CONFLICT(device_id) DO UPDATE SET device_id=EXCLUDED.device_id RETURNING id',
        [body.deviceId],
      );
      const anonymousUserId = user.rows[0].id;

      const existingDevice = await client.query(
        'SELECT id FROM devices WHERE anonymous_user_id=$1 ORDER BY updated_at DESC LIMIT 1',
        [anonymousUserId],
      );

      let deviceId: string;
      if (existingDevice.rows[0]) {
        const device = await client.query(
          'UPDATE devices SET platform=$2, app_version=$3, os_version=$4, timezone=$5, updated_at=now() WHERE id=$1 RETURNING id',
          [existingDevice.rows[0].id, body.platform, body.appVersion, body.osVersion ?? null, body.timezone],
        );
        deviceId = device.rows[0].id;
      } else {
        const device = await client.query(
          'INSERT INTO devices(anonymous_user_id, platform, app_version, os_version, timezone) VALUES ($1,$2,$3,$4,$5) RETURNING id',
          [anonymousUserId, body.platform, body.appVersion, body.osVersion ?? null, body.timezone],
        );
        deviceId = device.rows[0].id;
      }

      const rawToken = randomBytes(32).toString('base64url');
      const tokenHash = createHash('sha256').update(rawToken).digest('hex');

      await client.query(
        'UPDATE auth_sessions SET expires_at=now() WHERE anonymous_user_id=$1 AND expires_at>now()',
        [anonymousUserId],
      );
      await client.query(
        'INSERT INTO auth_sessions(anonymous_user_id, token_hash, expires_at) VALUES ($1,$2,now() + interval \'30 days\')',
        [anonymousUserId, tokenHash],
      );

      return {
        anonymousUserId,
        deviceId,
        accessToken: rawToken,
        tokenType: 'Bearer',
        expiresInDays: 30,
        authMode: 'anonymous',
        syncEnabled: true,
      };
    });
  }
}
