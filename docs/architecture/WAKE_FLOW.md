# AWERO Wake Flow

## Runtime

Alarm trigger -> WakeFlowController -> WakeSession -> MissionEngine -> completion/fallback.

The controller owns user-facing state transitions. Critical alarm execution remains local.

## States

IDLE
RINGING
MISSION
COMPLETED
EMERGENCY_STOPPED

## Controls

- Start mission
- Snooze, bounded by alarm policy
- Emergency Stop
- Complete mission

## Fallback

FallbackEngine provides deterministic local fallback:
- Photo -> QR
- QR -> Math
- Steps -> Math
- Mixed -> Math
- Math -> no further fallback

No backend or AI is required to execute fallback.
