-- A reported device identifier is metadata, not proof of ownership.
ALTER TABLE anonymous_users
  DROP CONSTRAINT IF EXISTS anonymous_users_device_id_key;

ALTER TABLE anonymous_users
  ADD COLUMN IF NOT EXISTS installation_secret_hash text;

CREATE UNIQUE INDEX IF NOT EXISTS anonymous_users_installation_secret_hash_idx
  ON anonymous_users(installation_secret_hash)
  WHERE installation_secret_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS alarms_owner_status_time_idx
  ON alarms(anonymous_user_id, status, hour, minute);
CREATE INDEX IF NOT EXISTS devices_owner_updated_idx
  ON devices(anonymous_user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS wake_sessions_alarm_scheduled_idx
  ON wake_sessions(alarm_id, scheduled_at DESC);

CREATE TABLE IF NOT EXISTS api_rate_limits (
  bucket_key text PRIMARY KEY,
  window_started_at timestamptz NOT NULL,
  request_count integer NOT NULL CHECK (request_count > 0)
);
CREATE INDEX IF NOT EXISTS api_rate_limits_window_idx ON api_rate_limits(window_started_at);

ALTER TABLE sync_operations DROP CONSTRAINT IF EXISTS sync_operations_outcome_check;
ALTER TABLE sync_operations ADD CONSTRAINT sync_operations_outcome_check
  CHECK (outcome IN ('LEGACY_ACKNOWLEDGED', 'PENDING', 'APPLIED', 'CONFLICT', 'REJECTED'));
