import { Body, Controller, Get, Headers, Post } from '@nestjs/common';
import { SyncBatchDto, SyncService } from './sync.service';

@Controller('sync')
export class SyncController {
  constructor(private readonly service: SyncService) {}

  @Get('alarms')
  serverAlarms(@Headers('authorization') authorization?: string) {
    return this.service.serverAlarms(authorization);
  }

  @Post()
  sync(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: SyncBatchDto,
  ) {
    return this.service.sync(authorization, body);
  }
}
