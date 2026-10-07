# Wake Session Contract

State machine:

SCHEDULED → TRIGGERED → AWAKE → MISSION → VALIDATED → COMPLETED

Failure path:

TRIGGERED → MISSION → MISSION_FAILED → FALLBACK → MISSION → COMPLETED

Emergency path:

TRIGGERED/MISSION → EMERGENCY_STOP

Snooze is tracked independently and cannot silently modify the configured alarm policy.
