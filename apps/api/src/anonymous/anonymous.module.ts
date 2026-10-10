import { Module } from '@nestjs/common';
import { AnonymousController } from './anonymous.controller';
import { AuthModule } from '../auth/auth.module';
import { AnonymousRegistrationService } from './anonymous-registration.service';

@Module({ imports: [AuthModule], controllers: [AnonymousController], providers: [AnonymousRegistrationService] })
export class AnonymousModule {}
