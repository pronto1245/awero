import { Module } from '@nestjs/common';
import { DatabaseModule } from '../database/database.module';
import { AuthModule } from '../auth/auth.module';
import { AlarmsController } from './alarms.controller';
import { AlarmService } from './alarms.service';

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [AlarmsController],
  providers: [AlarmService],
})
export class AlarmsModule {}
