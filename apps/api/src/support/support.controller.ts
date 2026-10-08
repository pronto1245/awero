import {
  IsEnum,
  IsDefined,
  IsInt,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  Max,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';
import { BadRequestException, Body, ConflictException, Controller, Headers, Post } from '@nestjs/common';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

enum SupportCategory {
  ALARM = 'ALARM',
  MISSION = 'MISSION',
  SYNC = 'SYNC',
  OTHER = 'OTHER',
}

enum ClientPlatform {
  IOS = 'IOS',
  ANDROID = 'ANDROID',
}

class DiagnosticsDto {
  @IsOptional()
  @IsString()
  @MaxLength(40)
  appVersion?: string;

  @IsOptional()
  @IsEnum(ClientPlatform)
  platform?: ClientPlatform;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  osVersion?: string;

  @IsOptional()
  @IsString()
  @MaxLength(64)
  timezone?: string;

  @IsOptional()
  @IsUUID()
  alarmId?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  alarmVersion?: number;

  @IsOptional()
  @IsString()
  @MaxLength(80)
  errorCode?: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(100_000)
  pendingSyncCount?: number;
}

class CreateSupportTicketDto {
  @IsUUID()
  id!: string;

  @IsEnum(SupportCategory)
  category!: SupportCategory;

  @IsDefined()
  @IsObject()
  @ValidateNested()
  @Type(() => DiagnosticsDto)
  diagnostics!: DiagnosticsDto;
}

@Controller('support')
export class SupportController {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  @Post('diagnostics')
  async submitDiagnostics(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: CreateSupportTicketDto,
  ) {
    const owner = await this.auth.resolve(authorization);
    if (body.diagnostics.timezone) {
      try {
        new Intl.DateTimeFormat('en-US', { timeZone: body.diagnostics.timezone });
      } catch {
        throw new BadRequestException('INVALID_TIMEZONE');
      }
    }
    const diagnostics = JSON.stringify(body.diagnostics);
    if (Buffer.byteLength(diagnostics, 'utf8') > 16_384) throw new BadRequestException('DIAGNOSTICS_TOO_LARGE');

    return this.db.transaction(async (client) => {
      const inserted = await client.query(
        `INSERT INTO support_tickets(id,anonymous_user_id,category,diagnostics)
         VALUES ($1,$2,$3,$4::jsonb) ON CONFLICT (id) DO NOTHING RETURNING id,status,created_at AS "createdAt"`,
        [body.id, owner.anonymousUserId, body.category, diagnostics],
      );
      if (inserted.rows[0]) return { item: inserted.rows[0], duplicate: false };

      const existing = await client.query(
        `SELECT id,status,created_at AS "createdAt",anonymous_user_id AS "anonymousUserId",category,
          diagnostics=$2::jsonb AS "sameDiagnostics"
         FROM support_tickets WHERE id=$1`,
        [body.id, diagnostics],
      );
      const row = existing.rows[0];
      if (!row || row.anonymousUserId !== owner.anonymousUserId || row.category !== body.category || !row.sameDiagnostics) {
        throw new ConflictException('SUPPORT_TICKET_ID_CONFLICT');
      }
      return { item: { id: row.id, status: row.status, createdAt: row.createdAt }, duplicate: true };
    });
  }
}
