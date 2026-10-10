# AWERO AI Schema Policy (Documentation Only)

This file records the intended validation policy; it is not executable schema code. No runtime AI schema package is implemented yet. Runtime validation is assigned to Phase 9 and must use a real schema in the production path before AI results are accepted.

All AI output is treated as untrusted input.

Required AI recommendation fields:
- mission_type
- difficulty
- reason_code
- confidence

Optional:
- steps_target

Policy engine must validate:
- mission is available on device
- difficulty is within user limits
- steps target is bounded
- no alarm time changes
- no subscription changes
- no permission escalation
