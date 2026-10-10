import { Body, Controller, Headers, Post } from '@nestjs/common';
import { AnalyticsService, AnalyticsBatchDto } from './analytics.service';

@Controller('analytics')
export class AnalyticsController {
  constructor(private readonly analytics: AnalyticsService) {}

  @Post('events')
  ingest(@Headers('authorization') authorization: string | undefined, @Body() body: AnalyticsBatchDto) {
    return this.analytics.ingest(authorization, body);
  }
}
