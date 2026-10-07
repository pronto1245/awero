import { Body, Controller, Headers, Post } from '@nestjs/common';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

type SyncOperation = {
  operationId: string;
  entityType: string;
  entityId: string;
  operationType: string;
  clientVersion?: number;
  payload: Record<string, unknown>;
  occurredAt?: string;
};

@Controller('api/v1/sync')
export class SyncController {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  @Post()
  async sync(@Headers('authorization') authorization: string | undefined, @Body() body: { operations?: SyncOperation[] }) {
    const owner = await this.auth.resolve(authorization);
    const operations = Array.isArray(body.operations) ? body.operations.slice(0, 100) : [];

    await this.db.transaction(async client => {
      for (const op of operations) {
        if (!op.operationId || !op.entityType || !op.entityId || !op.operationType) continue;
        await client.query(
          'INSERT INTO sync_operations(id, anonymous_user_id, device_id, operation_type, entity_type, entity_id, client_version, payload, occurred_at) VALUES ($1,$2,(SELECT id FROM devices WHERE anonymous_user_id=$2 ORDER BY updated_at DESC LIMIT 1),$3,$4,$5,$6,$7,COALESCE($8::timestamptz,now())) ON CONFLICT DO NOTHING',
          [op.operationId, owner.anonymousUserId, op.operationType, op.entityType, op.entityId, op.clientVersion ?? null, op.payload ?? {}, op.occurredAt ?? null],
        );
      }
    });

    return { accepted: operations.length, serverTime: new Date().toISOString() };
  }
}
