import { Body, Controller, Get, Headers, Param, ParseUUIDPipe, Post } from '@nestjs/common';
import { CreateWakeEventDto, CreateWakeSessionDto, WakeSessionsService } from './wake-sessions.service';

@Controller('wake-sessions')
export class WakeSessionsController {
  constructor(private readonly sessions: WakeSessionsService) {}

  @Get()
  list(@Headers('authorization') authorization?: string) {
    return this.sessions.list(authorization);
  }

  @Post()
  create(@Headers('authorization') authorization: string | undefined, @Body() body: CreateWakeSessionDto) {
    return this.sessions.create(authorization, body);
  }

  @Post(':id/events')
  appendEvent(
    @Headers('authorization') authorization: string | undefined,
    @Param('id', new ParseUUIDPipe()) id: string,
    @Body() body: CreateWakeEventDto,
  ) {
    return this.sessions.appendEvent(authorization, id, body);
  }

  @Get(':id/events')
  events(
    @Headers('authorization') authorization: string | undefined,
    @Param('id', new ParseUUIDPipe()) id: string,
  ) {
    return this.sessions.events(authorization, id);
  }
}
