import { Controller, Get } from '@nestjs/common';
import { DatabaseService } from '../database/database.service';

@Controller('health')
export class HealthController {
  constructor(private readonly database: DatabaseService) {}

  @Get()
  async getHealth() {
    let database = 'ok';
    try {
      await this.database.query('SELECT 1');
    } catch {
      database = 'unavailable';
    }

    return {
      status: database === 'ok' ? 'ok' : 'degraded',
      service: 'awero-api',
      version: 'v1',
      database,
      timestamp: new Date().toISOString(),
    };
  }
}
