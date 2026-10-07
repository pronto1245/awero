import { Module } from '@nestjs/common';
import { AnonymousController } from './anonymous.controller';

@Module({ controllers: [AnonymousController] })
export class AnonymousModule {}
