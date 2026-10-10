ALTER TABLE alarms
  ADD COLUMN version integer NOT NULL DEFAULT 1 CHECK (version > 0);

INSERT INTO alarm_versions(alarm_id, version, snapshot)
SELECT id, version, to_jsonb(alarms)
FROM alarms
ON CONFLICT (alarm_id, version) DO NOTHING;
