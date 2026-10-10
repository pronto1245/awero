import { Injectable, UnauthorizedException } from '@nestjs/common';
import { createHash } from 'crypto';
import { DatabaseService } from '../database/database.service';

@Injectable()
export class AnonymousAuthService {
  constructor(private readonly db: DatabaseService) {}

  async resolve(authorization?: string) {
    const match = authorization?.trim().match(/^Bearer\s+([A-Za-z0-9_-]{43})$/i);
    const token = match?.[1];
    if (!token) throw new UnauthorizedException('AUTH_REQUIRED');
    const hash = createHash('sha256').update(token).digest('hex');
    const result = await this.db.query(
      'SELECT anonymous_user_id AS "anonymousUserId", id AS "sessionId" FROM auth_sessions WHERE token_hash=$1 AND expires_at>now() AND anonymous_user_id IS NOT NULL',
      [hash],
    );
    if (!result.rows[0]) throw new UnauthorizedException('INVALID_TOKEN');
    return result.rows[0] as { anonymousUserId: string; sessionId: string };
  }
}
