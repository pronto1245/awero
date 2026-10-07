import { Module } from '@nestjs/common';
import { DatabaseModule } from '../database/database.module';
import { AuthModule } from '../auth/auth.module';
import { AlarmsController } from './alarms.controller';

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [AlarmsController],
})
export class AlarmsModule {}
