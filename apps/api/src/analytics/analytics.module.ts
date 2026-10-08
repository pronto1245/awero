import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { AnalyticsController } from './analytics.controller';

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [AnalyticsController],
})
export class AnalyticsModule {}
