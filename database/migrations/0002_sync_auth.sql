CREATE TABLE auth_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  anonymous_user_id uuid REFERENCES anonymous_users(id) ON DELETE CASCADE,
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  token_hash text NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK (anonymous_user_id IS NOT NULL OR user_id IS NOT NULL)
);

CREATE INDEX auth_sessions_expires_idx ON auth_sessions(expires_at);

CREATE TABLE sync_operations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  anonymous_user_id uuid REFERENCES anonymous_users(id) ON DELETE CASCADE,
  user_id uuid REFERENCES users(id) ON DELETE CASCADE,
  device_id uuid REFERENCES devices(id) ON DELETE CASCADE,
  operation_type text NOT NULL,
  entity_type text NOT NULL,
  entity_id text NOT NULL,
  client_version integer,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  occurred_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK (anonymous_user_id IS NOT NULL OR user_id IS NOT NULL)
);

CREATE INDEX sync_operations_owner_idx ON sync_operations(anonymous_user_id, user_id);
CREATE INDEX sync_operations_entity_idx ON sync_operations(entity_type, entity_id);

ALTER TABLE analytics_events
  ADD COLUMN IF NOT EXISTS payload jsonb NOT NULL DEFAULT '{}'::jsonb;

CREATE INDEX IF NOT EXISTS analytics_events_name_time_idx
  ON analytics_events(event_name, occurred_at DESC);
