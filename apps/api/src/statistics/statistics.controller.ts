import { Controller, Get, Headers } from '@nestjs/common';
import { StatisticsService } from './statistics.service';

@Controller('statistics')
export class StatisticsController {
  constructor(private readonly statistics: StatisticsService) {}

  @Get('summary')
  summary(@Headers('authorization') authorization?: string) {
    return this.statistics.summary(authorization);
  }
}
