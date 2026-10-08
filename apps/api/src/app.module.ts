import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { DatabaseModule } from './database/database.module';
import { HealthModule } from './health/health.module';
import { AnonymousModule } from './anonymous/anonymous.module';
import { AuthModule } from './auth/auth.module';
import { AlarmsModule } from './alarms/alarms.module';
import { WakeSessionsModule } from './wake-sessions/wake-sessions.module';
import { StatisticsModule } from './statistics/statistics.module';
import { SyncModule } from './sync/sync.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    DatabaseModule,
    HealthModule,
    AnonymousModule,
    AuthModule,
    AlarmsModule,
    WakeSessionsModule,
    StatisticsModule,
    SyncModule,
  ],
})
export class AppModule {}
