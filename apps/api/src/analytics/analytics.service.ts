import {
  Allow,
  ArrayMaxSize,
  IsArray,
  IsInt,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  Matches,
  Max,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';
import { BadRequestException, ConflictException, HttpException, Injectable } from '@nestjs/common';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

class AnalyticsEventDto {
  @IsUUID()
  id!: string;

  @IsString()
  @Matches(/^[a-zA-Z][a-zA-Z0-9_.-]*$/)
  @MaxLength(100)
  eventName!: string;

  @IsInt()
  @Min(1)
  @Max(100)
  eventVersion!: number;

  @IsOptional()
  @IsObject()
  properties?: Record<string, unknown>;

  @Allow()
  occurredAt?: unknown;
}

export class AnalyticsBatchDto {
  @IsArray()
  @ArrayMaxSize(100)
  @ValidateNested({ each: true })
  @Type(() => AnalyticsEventDto)
  events!: AnalyticsEventDto[];
}

@Injectable()
export class AnalyticsService {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  async ingest(authorization: string | undefined, body: AnalyticsBatchDto) {
    const owner = await this.auth.resolve(authorization);
    const response = await this.db.transaction(async (client) => {
      const acceptedIds: string[] = [];
      const rejected: Array<{ id: string; code: string }> = [];
      for (const event of body.events) {
        const savepoint = `analytics_${acceptedIds.length}_${rejected.length}`;
        await client.query(`SAVEPOINT ${savepoint}`);
        try {
        const properties = event.properties ?? {};
        if (Buffer.byteLength(JSON.stringify(properties), 'utf8') > 16_384) {
          throw new BadRequestException('ANALYTICS_PROPERTIES_TOO_LARGE');
        }
        const occurredAt = this.parseOccurredAt(event.occurredAt);
        const retryOccurredAt = event.occurredAt === undefined || event.occurredAt === null
          ? null
          : occurredAt;
        const inserted = await client.query(
          `INSERT INTO analytics_events(id,anonymous_user_id,device_id,event_name,event_version,occurred_at,properties)
           VALUES ($1,$2,(SELECT id FROM devices WHERE anonymous_user_id=$2 ORDER BY updated_at DESC LIMIT 1),$3,$4,$5,$6::jsonb)
           ON CONFLICT (id) DO NOTHING RETURNING id`,
          [event.id, owner.anonymousUserId, event.eventName, event.eventVersion, occurredAt, JSON.stringify(properties)],
        );
        if (inserted.rows[0]) {
          acceptedIds.push(event.id);
          await client.query(`RELEASE SAVEPOINT ${savepoint}`);
          continue;
        }
        const existing = await client.query(
          `SELECT anonymous_user_id AS "anonymousUserId",event_name AS "eventName",event_version AS "eventVersion",
             ($2::timestamptz IS NULL OR occurred_at=$2::timestamptz) AS "sameTime",properties=$3::jsonb AS "sameProperties"
           FROM analytics_events WHERE id=$1`,
          [event.id, retryOccurredAt, JSON.stringify(properties)],
        );
        const row = existing.rows[0];
        if (
          !row || row.anonymousUserId !== owner.anonymousUserId || row.eventName !== event.eventName ||
          row.eventVersion !== event.eventVersion || !row.sameTime || !row.sameProperties
        ) throw new ConflictException('ANALYTICS_EVENT_ID_CONFLICT');
        acceptedIds.push(event.id);
        await client.query(`RELEASE SAVEPOINT ${savepoint}`);
        } catch (error) {
          const code = this.rejectionCode(error);
          if (!code) throw error;
          await client.query(`ROLLBACK TO SAVEPOINT ${savepoint}`);
          rejected.push({ id: event.id, code });
          await client.query(`RELEASE SAVEPOINT ${savepoint}`);
        }
      }
      return { acceptedIds, rejected };
    });
    return { ...response, accepted: response.acceptedIds.length, serverTime: new Date().toISOString() };
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
      throw new BadRequestException('INVALID_ANALYTICS_OCCURRED_AT');
    }
    if (!Number.isFinite(date.getTime())) throw new BadRequestException('INVALID_ANALYTICS_OCCURRED_AT');
    const now = Date.now();
    if (date.getTime() < now - 30 * 24 * 60 * 60 * 1000 || date.getTime() > now + 5 * 60 * 1000) {
      throw new BadRequestException('ANALYTICS_OCCURRED_AT_OUT_OF_RANGE');
    }
    return date;
  }

  private rejectionCode(error: unknown): string | null {
    if (error instanceof HttpException && error.getStatus() >= 400 && error.getStatus() < 500) {
      const response = error.getResponse();
      if (typeof response === 'string') return response;
      if (response && typeof response === 'object' && 'message' in response) {
        const message = (response as { message: unknown }).message;
        return typeof message === 'string' ? message : 'INVALID_ANALYTICS_EVENT';
      }
      return 'INVALID_ANALYTICS_EVENT';
    }
    if (['23505', '23514', '22P02'].includes((error as { code?: string })?.code ?? '')) {
      return 'INVALID_ANALYTICS_EVENT';
    }
    return null;
  }
}
