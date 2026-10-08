import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { DatabaseModule } from '../database/database.module';
import { StatisticsController } from './statistics.controller';

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [StatisticsController],
})
export class StatisticsModule {}
