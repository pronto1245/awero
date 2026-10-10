import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { WakeSessionsController } from './wake-sessions.controller';
import { WakeSessionsService } from './wake-sessions.service';

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [WakeSessionsController],
  providers: [WakeSessionsService],
})
export class WakeSessionsModule {}
