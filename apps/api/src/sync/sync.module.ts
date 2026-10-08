import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { SyncController } from './sync.controller';

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [SyncController],
})
export class SyncModule {}
