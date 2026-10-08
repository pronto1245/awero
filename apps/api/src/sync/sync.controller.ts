import { Allow, ArrayMaxSize, IsArray, IsInt, IsNotEmpty, IsObject, IsOptional, IsString, IsUUID, MaxLength, Min, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';
import { BadRequestException, ConflictException, Controller, Headers, Post, Body } from '@nestjs/common';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

class SyncOperationDto {
  @IsUUID()
  id!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(32)
  operationType!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(32)
  entityType!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(256)
  entityId!: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  clientVersion?: number;

  @IsOptional()
  @IsObject()
  payload?: Record<string, unknown>;

  @Allow()
  occurredAt?: unknown;
}

class SyncBatchDto {
  @IsArray()
  @ArrayMaxSize(100)
  @ValidateNested({ each: true })
  @Type(() => SyncOperationDto)
  operations!: SyncOperationDto[];
}

@Controller('sync')
export class SyncController {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  @Post()
  async sync(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: SyncBatchDto,
  ) {
    const owner = await this.auth.resolve(authorization);
    const acceptedIds = await this.db.transaction(async (client) => {
      const result: string[] = [];
      for (const operation of body.operations) {
        const occurredAt = this.parseOccurredAt(operation.occurredAt);
        const payload = operation.payload ?? {};
        const inserted = await client.query(
          `INSERT INTO sync_operations(id,anonymous_user_id,device_id,operation_type,entity_type,entity_id,client_version,payload,occurred_at)
           VALUES ($1,$2,(SELECT id FROM devices WHERE anonymous_user_id=$2 ORDER BY updated_at DESC LIMIT 1),$3,$4,$5,$6,$7::jsonb,$8)
           ON CONFLICT (id) DO NOTHING RETURNING id`,
          [operation.id, owner.anonymousUserId, operation.operationType, operation.entityType, operation.entityId,
            operation.clientVersion ?? null, JSON.stringify(payload), occurredAt],
        );
        if (inserted.rows[0]) {
          result.push(operation.id);
          continue;
        }
        const existing = await client.query(
          `SELECT anonymous_user_id AS "anonymousUserId",operation_type AS "operationType",entity_type AS "entityType",
             entity_id AS "entityId",client_version AS "clientVersion",payload=$2::jsonb AS "samePayload",
             occurred_at=$3::timestamptz AS "sameTime"
           FROM sync_operations WHERE id=$1`,
          [operation.id, JSON.stringify(payload), occurredAt],
        );
        const row = existing.rows[0];
        if (
          !row || row.anonymousUserId !== owner.anonymousUserId || row.operationType !== operation.operationType ||
          row.entityType !== operation.entityType || row.entityId !== operation.entityId ||
          row.clientVersion !== (operation.clientVersion ?? null) || !row.samePayload || !row.sameTime
        ) throw new ConflictException('SYNC_OPERATION_ID_CONFLICT');
        result.push(operation.id);
      }
      return result;
    });

    return { acceptedIds, accepted: acceptedIds.length, serverTime: new Date().toISOString() };
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
      throw new BadRequestException('INVALID_SYNC_OCCURRED_AT');
    }
    if (!Number.isFinite(date.getTime())) throw new BadRequestException('INVALID_SYNC_OCCURRED_AT');
    return date;
  }
}
