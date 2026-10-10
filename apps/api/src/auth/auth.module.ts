import { Module } from '@nestjs/common';
import { DatabaseModule } from '../database/database.module';
import { AnonymousAuthService } from './anonymous-auth.service';

@Module({
  imports: [DatabaseModule],
  providers: [AnonymousAuthService],
  exports: [AnonymousAuthService],
})
export class AuthModule {}
