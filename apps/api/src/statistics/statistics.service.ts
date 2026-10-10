import { Injectable } from '@nestjs/common';
import { AnonymousAuthService } from '../auth/anonymous-auth.service';
import { DatabaseService } from '../database/database.service';

@Injectable()
export class StatisticsService {
  constructor(private readonly db: DatabaseService, private readonly auth: AnonymousAuthService) {}

  async summary(authorization?: string) {
    const owner = await this.auth.resolve(authorization);
    const device = await this.db.query(
      'SELECT timezone FROM devices WHERE anonymous_user_id=$1 ORDER BY updated_at DESC LIMIT 1',
      [owner.anonymousUserId],
    );
    const timezone = this.validTimezone(device.rows[0]?.timezone) ? device.rows[0].timezone : 'UTC';

    const [aggregate, successfulDays] = await Promise.all([
      this.db.query(
        `SELECT COUNT(*)::integer AS "totalWakes",
          COUNT(*) FILTER (WHERE result IS NOT NULL)::integer AS "finishedWakes",
          COUNT(*) FILTER (WHERE result='COMPLETED')::integer AS "successfulWakes",
          COUNT(*) FILTER (WHERE result='FAILED')::integer AS "failedWakes",
          COUNT(*) FILTER (WHERE result='EMERGENCY_STOP')::integer AS "emergencyStops",
          COUNT(*) FILTER (WHERE snooze_count>0)::integer AS "snoozedWakes",
          AVG(completion_time_seconds) FILTER (WHERE result='COMPLETED') AS "averageCompletionSeconds"
         FROM wake_sessions ws JOIN alarms a ON a.id=ws.alarm_id
         WHERE a.anonymous_user_id=$1`,
        [owner.anonymousUserId],
      ),
      this.db.query(
        `SELECT DISTINCT ((ws.scheduled_at AT TIME ZONE
          CASE WHEN a.timezone_mode='FIXED' THEN a.fixed_timezone ELSE $2 END)::date)::text AS day
         FROM wake_sessions ws JOIN alarms a ON a.id=ws.alarm_id
         WHERE a.anonymous_user_id=$1 AND ws.result='COMPLETED'
         ORDER BY day DESC`,
        [owner.anonymousUserId, timezone],
      ),
    ]);

    const summary = aggregate.rows[0];
    const successDays = successfulDays.rows.map((row) => row.day as string);
    const streak = this.calculateStreak(successDays, timezone);
    const successfulWakes = Number(summary.successfulWakes);
    const finishedWakes = Number(summary.finishedWakes);

    return {
      generatedAt: new Date().toISOString(),
      timezone,
      summary: {
        totalWakes: Number(summary.totalWakes),
        finishedWakes,
        successfulWakes,
        failedWakes: Number(summary.failedWakes),
        emergencyStops: Number(summary.emergencyStops),
        snoozedWakes: Number(summary.snoozedWakes),
        successRatePercent: finishedWakes === 0 ? 0 : Math.round((successfulWakes / finishedWakes) * 10000) / 100,
        averageCompletionSeconds:
          summary.averageCompletionSeconds === null ? null : Math.round(Number(summary.averageCompletionSeconds)),
      },
      streak,
    };
  }

  private calculateStreak(successDays: string[], timezone: string) {
    let best = 0;
    let run = 0;
    let latestRun = 0;
    let previous: number | null = null;
    for (const [index, day] of successDays.entries()) {
      const value = Date.parse(`${day}T00:00:00.000Z`);
      run = previous !== null && previous - value === 86_400_000 ? run + 1 : 1;
      if (index === 0) latestRun = 1;
      else if (latestRun > 0) latestRun = previous! - value === 86_400_000 ? latestRun + 1 : 0;
      best = Math.max(best, run);
      previous = value;
    }

    const today = this.localDate(new Date(), timezone);
    const latest = successDays[0];
    const yesterday = new Date(`${today}T00:00:00.000Z`);
    yesterday.setUTCDate(yesterday.getUTCDate() - 1);
    const current = latest === today || latest === yesterday.toISOString().slice(0, 10) ? latestRun : 0;
    return { current, best };
  }

  private localDate(date: Date, timezone: string): string {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(date);
    const part = (type: string) => parts.find((item) => item.type === type)?.value ?? '00';
    return `${part('year')}-${part('month')}-${part('day')}`;
  }

  private validTimezone(timezone: string | undefined): boolean {
    if (!timezone) return false;
    try {
      new Intl.DateTimeFormat('en-US', { timeZone: timezone });
      return true;
    } catch {
      return false;
    }
  }
}
