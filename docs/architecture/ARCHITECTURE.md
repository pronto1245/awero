# AWERO Architecture

## Core rule

**Local-First Mobile Product + Cloud Intelligence.**

The phone is authoritative for the next critical alarm and wake session execution.

Backend responsibilities:
- identity
- sync
- statistics
- subscriptions
- support
- analytics
- AI recommendations

AI never directly executes device actions. Recommendation flow:

AI → schema validation → policy engine → allowed action → device.

## Critical path

Alarm creation:
Local DB → Alarm Scheduler → OS alarm.

Wake:
OS alarm → Wake Session → Mission Engine → Validation → Fallback → Completion → local statistics → eventual sync.

## Reliability

Critical wake behavior must continue when:
- internet is unavailable
- API is unavailable
- AI is unavailable
- account is anonymous
- subscription has expired

Remote configuration cannot disable the critical alarm path, fallback or emergency stop.
