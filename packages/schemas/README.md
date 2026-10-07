# AWERO Schemas

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
