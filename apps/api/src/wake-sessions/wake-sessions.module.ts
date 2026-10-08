import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { WakeSessionsController } from './wake-sessions.controller';

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [WakeSessionsController],
})
export class WakeSessionsModule {}
