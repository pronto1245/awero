import { Body, Controller, Delete, Get, Headers, Param, ParseUUIDPipe, Patch, Post } from '@nestjs/common';
import { AlarmService, CreateAlarmDto, UpdateAlarmDto } from './alarms.service';

@Controller('alarms')
export class AlarmsController {
  constructor(private readonly alarms: AlarmService) {}

  @Get()
  list(@Headers('authorization') authorization?: string) {
    return this.alarms.list(authorization);
  }

  @Post()
  create(@Headers('authorization') authorization: string | undefined, @Body() body: CreateAlarmDto) {
    return this.alarms.create(authorization, body);
  }

  @Patch(':id')
  update(
    @Headers('authorization') authorization: string | undefined,
    @Param('id', new ParseUUIDPipe()) id: string,
    @Body() body: UpdateAlarmDto,
  ) {
    return this.alarms.update(authorization, id, body);
  }

  @Delete(':id')
  remove(
    @Headers('authorization') authorization: string | undefined,
    @Param('id', new ParseUUIDPipe()) id: string,
  ) {
    return this.alarms.remove(authorization, id);
  }
}
