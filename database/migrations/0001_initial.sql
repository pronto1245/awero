CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE mission_type AS ENUM ('MATH','QR','STEPS','PHOTO','MIXED');
CREATE TYPE difficulty AS ENUM ('EASY','MEDIUM','HARD');
CREATE TYPE timezone_mode AS ENUM ('DEVICE_LOCAL','FIXED');
CREATE TYPE alarm_status AS ENUM ('ACTIVE','PAUSED','DELETED');
CREATE TYPE wake_result AS ENUM ('COMPLETED','FAILED','EMERGENCY_STOP','CANCELLED');
CREATE TYPE subscription_status AS ENUM ('FREE','TRIALING','ACTIVE','EXPIRED','CANCELLED');

CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text UNIQUE,
  display_name text,
  locale text NOT NULL DEFAULT 'en',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

CREATE TABLE anonymous_users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  device_id text NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now(),
  converted_user_id uuid REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE devices (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  anonymous_user_id uuid REFERENCES anonymous_users(id) ON DELETE CASCADE,
  platform text NOT NULL,
  app_version text NOT NULL,
  os_version text,
  timezone text NOT NULL,
  push_token text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK (user_id IS NOT NULL OR anonymous_user_id IS NOT NULL)
);

CREATE TABLE user_settings (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  locale text NOT NULL DEFAULT 'en',
  timezone text NOT NULL DEFAULT 'UTC',
  default_snooze_minutes integer NOT NULL DEFAULT 10 CHECK (default_snooze_minutes BETWEEN 1 AND 60),
  max_snoozes integer NOT NULL DEFAULT 3 CHECK (max_snoozes BETWEEN 0 AND 20),
  dark_mode text NOT NULL DEFAULT 'system',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE alarms (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  anonymous_user_id uuid REFERENCES anonymous_users(id) ON DELETE CASCADE,
  label text NOT NULL DEFAULT 'Alarm',
  hour smallint NOT NULL CHECK (hour BETWEEN 0 AND 23),
  minute smallint NOT NULL CHECK (minute BETWEEN 0 AND 59),
  timezone_mode timezone_mode NOT NULL DEFAULT 'DEVICE_LOCAL',
  fixed_timezone text,
  status alarm_status NOT NULL DEFAULT 'ACTIVE',
  snooze_enabled boolean NOT NULL DEFAULT true,
  max_snoozes integer NOT NULL DEFAULT 3 CHECK (max_snoozes BETWEEN 0 AND 20),
  snooze_minutes integer NOT NULL DEFAULT 10 CHECK (snooze_minutes BETWEEN 1 AND 60),
  mission_type mission_type NOT NULL DEFAULT 'MATH',
  difficulty difficulty NOT NULL DEFAULT 'MEDIUM',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK (user_id IS NOT NULL OR anonymous_user_id IS NOT NULL),
  CHECK (timezone_mode <> 'FIXED' OR fixed_timezone IS NOT NULL)
);

CREATE TABLE alarm_versions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  alarm_id uuid NOT NULL REFERENCES alarms(id) ON DELETE CASCADE,
  version integer NOT NULL,
  snapshot jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(alarm_id, version)
);

CREATE TABLE alarm_schedules (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  alarm_id uuid NOT NULL REFERENCES alarms(id) ON DELETE CASCADE,
  alarm_version integer NOT NULL,
  scheduled_for timestamptz NOT NULL,
  recurrence_mask smallint NOT NULL DEFAULT 127,
  timezone text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(alarm_id, scheduled_for)
);

CREATE TABLE missions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  alarm_id uuid NOT NULL REFERENCES alarms(id) ON DELETE CASCADE,
  type mission_type NOT NULL,
  difficulty difficulty NOT NULL,
  config jsonb NOT NULL DEFAULT '{}'::jsonb,
  fallback_type mission_type,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE wake_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  alarm_id uuid NOT NULL REFERENCES alarms(id) ON DELETE CASCADE,
  alarm_version integer NOT NULL,
  scheduled_at timestamptz NOT NULL,
  triggered_at timestamptz,
  mission_started_at timestamptz,
  completed_at timestamptz,
  result wake_result,
  mission_type mission_type NOT NULL,
  completion_time_seconds integer,
  snooze_count integer NOT NULL DEFAULT 0,
  fallback_used boolean NOT NULL DEFAULT false,
  emergency_stop boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE mission_attempts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  wake_session_id uuid NOT NULL REFERENCES wake_sessions(id) ON DELETE CASCADE,
  mission_id uuid REFERENCES missions(id) ON DELETE SET NULL,
  attempt_number integer NOT NULL,
  started_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz,
  result text,
  failure_code text,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE wake_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  wake_session_id uuid NOT NULL REFERENCES wake_sessions(id) ON DELETE CASCADE,
  event_type text NOT NULL,
  occurred_at timestamptz NOT NULL DEFAULT now(),
  payload jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE wake_statistics (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  planned_wakes integer NOT NULL DEFAULT 0,
  successful_wakes integer NOT NULL DEFAULT 0,
  snoozed_wakes integer NOT NULL DEFAULT 0,
  failed_wakes integer NOT NULL DEFAULT 0,
  total_completion_seconds bigint NOT NULL DEFAULT 0,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE streaks (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  current_streak integer NOT NULL DEFAULT 0,
  best_streak integer NOT NULL DEFAULT 0,
  last_success_date date,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ai_profiles (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  preferred_missions jsonb NOT NULL DEFAULT '[]'::jsonb,
  mission_success_rates jsonb NOT NULL DEFAULT '{}'::jsonb,
  snooze_rate numeric(6,5) NOT NULL DEFAULT 0,
  confidence numeric(6,5) NOT NULL DEFAULT 0,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ai_recommendations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  alarm_id uuid REFERENCES alarms(id) ON DELETE SET NULL,
  mission_type mission_type NOT NULL,
  difficulty difficulty NOT NULL,
  parameters jsonb NOT NULL DEFAULT '{}'::jsonb,
  reason_code text NOT NULL,
  confidence numeric(6,5) NOT NULL,
  accepted boolean,
  applied_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status subscription_status NOT NULL DEFAULT 'FREE',
  product_id text,
  platform text,
  expires_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE subscription_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  subscription_id uuid NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
  event_type text NOT NULL,
  external_id text,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(subscription_id, external_id)
);

CREATE TABLE analytics_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid REFERENCES users(id) ON DELETE SET NULL,
  anonymous_user_id uuid REFERENCES anonymous_users(id) ON DELETE SET NULL,
  device_id uuid REFERENCES devices(id) ON DELETE SET NULL,
  event_name text NOT NULL,
  event_version integer NOT NULL DEFAULT 1,
  occurred_at timestamptz NOT NULL DEFAULT now(),
  properties jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE support_tickets (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid REFERENCES users(id) ON DELETE SET NULL,
  anonymous_user_id uuid REFERENCES anonymous_users(id) ON DELETE SET NULL,
  category text NOT NULL,
  status text NOT NULL DEFAULT 'OPEN',
  diagnostics jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE feature_flags (
  key text PRIMARY KEY,
  enabled boolean NOT NULL DEFAULT false,
  config jsonb NOT NULL DEFAULT '{}'::jsonb,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
  action text NOT NULL,
  entity_type text NOT NULL,
  entity_id uuid,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX alarms_user_status_idx ON alarms(user_id, status);
CREATE INDEX alarm_schedules_due_idx ON alarm_schedules(scheduled_for);
CREATE INDEX wake_sessions_alarm_idx ON wake_sessions(alarm_id, scheduled_at DESC);
CREATE INDEX wake_events_session_idx ON wake_events(wake_session_id, occurred_at);
CREATE INDEX analytics_events_name_time_idx ON analytics_events(event_name, occurred_at);
CREATE INDEX support_tickets_status_idx ON support_tickets(status, created_at);
CREATE INDEX audit_logs_entity_idx ON audit_logs(entity_type, entity_id);
