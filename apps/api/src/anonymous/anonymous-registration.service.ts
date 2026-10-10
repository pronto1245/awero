import { BadRequestException, ConflictException, HttpException, Injectable } from '@nestjs/common';
import { createHash, randomBytes } from 'crypto';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

export interface AnonymousRegistrationInput {
  deviceId: string;
  installationSecret: string;
  platform: string;
  appVersion: string;
  osVersion?: string;
  timezone: string;
}

@Injectable()
export class AnonymousRegistrationService {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  async register(body: AnonymousRegistrationInput, remoteAddress: string) {
    this.validateTimezone(body.timezone);
    await this.enforceRateLimit(remoteAddress, 'anonymous-registration', 10);
    const installationSecretHash = this.hash(body.installationSecret);

    return this.db.transaction(async (client) => {
      await client.query('SELECT pg_advisory_xact_lock(hashtextextended($1,0))', [installationSecretHash]);
      let user = await client.query<{ id: string }>(
        'SELECT id FROM anonymous_users WHERE installation_secret_hash=$1 FOR UPDATE',
        [installationSecretHash],
      );
      if (!user.rows[0]) {
        user = await client.query<{ id: string }>(
          'INSERT INTO anonymous_users(device_id,installation_secret_hash) VALUES ($1,$2) RETURNING id',
          [body.deviceId, installationSecretHash],
        );
      }
      const anonymousUserId = user.rows[0].id;
      const existingDevice = await client.query<{ id: string }>(
        'SELECT id FROM devices WHERE anonymous_user_id=$1 ORDER BY updated_at DESC LIMIT 1',
        [anonymousUserId],
      );
      let deviceId: string;
      if (existingDevice.rows[0]) {
        const device = await client.query<{ id: string }>(
          'UPDATE devices SET platform=$2,app_version=$3,os_version=$4,timezone=$5,updated_at=now() WHERE id=$1 RETURNING id',
          [existingDevice.rows[0].id, body.platform, body.appVersion, body.osVersion ?? null, body.timezone],
        );
        deviceId = device.rows[0].id;
      } else {
        const device = await client.query<{ id: string }>(
          'INSERT INTO devices(anonymous_user_id,platform,app_version,os_version,timezone) VALUES ($1,$2,$3,$4,$5) RETURNING id',
          [anonymousUserId, body.platform, body.appVersion, body.osVersion ?? null, body.timezone],
        );
        deviceId = device.rows[0].id;
      }

      const accessToken = randomBytes(32).toString('base64url');
      await client.query(
        "INSERT INTO auth_sessions(anonymous_user_id,token_hash,expires_at) VALUES ($1,$2,now() + interval '30 days')",
        [anonymousUserId, this.hash(accessToken)],
      );
      return {
        anonymousUserId,
        deviceId,
        accessToken,
        tokenType: 'Bearer',
        expiresInDays: 30,
        authMode: 'anonymous',
        syncEnabled: true,
      };
    });
  }

  async bindInstallation(authorization: string | undefined, installationSecret: string, remoteAddress: string) {
    await this.enforceRateLimit(remoteAddress, 'anonymous-credential-binding', 10);
    const owner = await this.auth.resolve(authorization);
    const secretHash = this.hash(installationSecret);
    try {
      const result = await this.db.query(
        `UPDATE anonymous_users SET installation_secret_hash=$2
         WHERE id=$1 AND (installation_secret_hash IS NULL OR installation_secret_hash=$2)
         RETURNING id`,
        [owner.anonymousUserId, secretHash],
      );
      if (!result.rowCount) throw new ConflictException('INSTALLATION_ALREADY_BOUND');
    } catch (error) {
      if ((error as { code?: string }).code === '23505') throw new ConflictException('INSTALLATION_ALREADY_BOUND');
      throw error;
    }
    return { bound: true };
  }

  private validateTimezone(timezone: string): void {
    try { new Intl.DateTimeFormat('en-US', { timeZone: timezone }); }
    catch { throw new BadRequestException('INVALID_REQUEST'); }
  }

  private async enforceRateLimit(remoteAddress: string, route: string, limit: number): Promise<void> {
    const bucket = this.hash(`${route}:${remoteAddress}`);
    const result = await this.db.query<{ request_count: number }>(
      `INSERT INTO api_rate_limits(bucket_key,window_started_at,request_count) VALUES ($1,now(),1)
       ON CONFLICT(bucket_key) DO UPDATE SET
         request_count=CASE WHEN api_rate_limits.window_started_at < now() - interval '1 hour' THEN 1
           ELSE api_rate_limits.request_count + 1 END,
         window_started_at=CASE WHEN api_rate_limits.window_started_at < now() - interval '1 hour' THEN now()
           ELSE api_rate_limits.window_started_at END RETURNING request_count`,
      [bucket],
    );
    const requestCount = Number(result.rows[0].request_count);
    if (requestCount % 10 === 0) {
      await this.db.query("DELETE FROM api_rate_limits WHERE window_started_at < now() - interval '2 hours'");
    }
    if (requestCount > limit) throw new HttpException('RATE_LIMITED', 429);
  }

  private hash(value: string): string {
    return createHash('sha256').update(value).digest('hex');
  }
}
