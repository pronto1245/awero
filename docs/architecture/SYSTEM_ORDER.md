# AWERO System Implementation Order

The single governing order is the phase table in [AWERO Product Specification and Implementation Plan](../AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md). Use it for dependencies, acceptance gates, and current status; this file must not define a second order.

## Agreed sequence

1. Product baseline and competitor-informed scope.
2. iOS and Android alarm reliability.
3. Wake-session runtime and P0 Math/Steps/QR missions.
4. Local persistence and recovery.
5. Backend and sync, isolated from the active alarm path.
6. UX completion and six-language localization.
7. Free/Pro billing.
8. P1 competitor capabilities: photo/object, exercise, cognitive missions, sequences, wake-up check, progress/history.
9. P2 optional weather/morning briefing, sleep, social, and adaptive/AI features.
10. Full automated and physical-device release validation.

The order protects the immediate user outcome: a scheduled alarm rings and the wake flow can complete safely. Exercise and weather remain in the full scope; visual redesign is deferred until the related behavior and acceptance criteria are ready. Green CI does not replace physical iOS/Android alarm tests.
