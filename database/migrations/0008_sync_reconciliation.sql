ALTER TABLE alarms
  ADD COLUMN weekdays smallint[] NOT NULL DEFAULT ARRAY[1,2,3,4,5,6,7]::smallint[];

ALTER TABLE sync_operations
  ADD COLUMN outcome text NOT NULL DEFAULT 'LEGACY_ACKNOWLEDGED',
  ADD COLUMN result jsonb NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE sync_operations
  ADD CONSTRAINT sync_operations_outcome_check
  CHECK (outcome IN ('LEGACY_ACKNOWLEDGED', 'PENDING', 'APPLIED', 'CONFLICT'));
