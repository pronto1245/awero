# AWERO MVP Release Plan

The authoritative phase order, product scope, current phase status, and acceptance gates live in [AWERO Product Specification and Implementation Plan](../AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md). Follow that document when implementing or updating status; this file is a short MVP entry point, not a competing roadmap.

## MVP outcome

A new user can install AWERO, start without an account, create and test an alarm, lock the phone, receive an audible alarm within documented OS limits, enter a wake mission, complete it or recover safely, see a truthful persisted result, and repeat the flow offline.

## P0 required before MVP

- Reliable native alarm delivery on iOS and Android, including permission education and truthful armed/failed state.
- Alarm edit/repeat/timezone/sound behavior and stale-schedule prevention.
- Wake flow with mission, retry, snooze, emergency stop, and recovery.
- Local Math, Steps, and QR/barcode missions with offline and permission-failure behavior.
- Durable local state and six-language user-facing coverage.
- Physical-device validation on both platforms. CI success or a push-only notification does not prove audible alarm delivery.

## Scope beyond MVP

P1/P2 features remain in the complete product scope in the governing specification: photo/object and exercise missions, sequences, wake-up checks, progress/history, optional weather briefing, sleep support, social accountability, and adaptive/AI recommendations. Their priority does not remove them from the roadmap.

## Release gate

Do not call AWERO MVP-ready until all P0 criteria pass on physical iPhone and Android devices and the required CI checks are green. Backend, billing, or optional services must never become dependencies for ringing or completing a wake mission.
