# AWERO API

NestJS modular monolith for anonymous identity, device registration, and alarm metadata. The mobile device remains authoritative for firing alarms.

## Start a fresh local backend

From the repository root, run:

```sh
docker compose -f infrastructure/docker/docker-compose.yml up --build
```

The API waits for PostgreSQL, applies numbered SQL migrations, and listens on `http://localhost:3000`. The health endpoint is `GET /api/v1/health`.

## Anonymous device registration

`POST /api/v1/auth/anonymous`

```json
{
  "deviceId": "a-random-stable-device-identifier",
  "platform": "IOS",
  "appVersion": "0.1.0",
  "osVersion": "18.0",
  "timezone": "Europe/Moscow"
}
```

Registration returns a 30-day bearer token. Store the token securely on the device and send it as `Authorization: Bearer <accessToken>` on private API requests. Only a hash of the token is stored in PostgreSQL.

## Alarm endpoints

- `GET /api/v1/alarms` — list the caller's non-deleted alarms
- `POST /api/v1/alarms` — create an alarm
- `PATCH /api/v1/alarms/:id` — update an alarm and append a new version snapshot
- `DELETE /api/v1/alarms/:id` — soft-delete an alarm and append a tombstone version

Create requests require integer `hour` (0–23) and `minute` (0–59). Optional fields are `label`, `timezoneMode`, `fixedTimezone`, `snoozeEnabled`, `maxSnoozes`, `snoozeMinutes`, `missionType`, `difficulty`, and `qrExpectedCode` (up to 2048 characters). Unknown properties and invalid enum/timezone values are rejected. QR alarms retain their exact expected code in alarm reads and version snapshots.

All API routes use the single global `/api/v1` prefix. Apply migrations through `pnpm --filter @awero/api db:migrate` for an empty database or through the local Docker Compose startup. Production schema changes remain migration-only.

## Wake session endpoints

- `GET /api/v1/wake-sessions` — list the caller's latest 100 sessions
- `POST /api/v1/wake-sessions` — create a client-generated session and its `TRIGGERED` event
- `POST /api/v1/wake-sessions/:id/events` — append one lifecycle event
- `GET /api/v1/wake-sessions/:id/events` — read the event history

The client supplies UUIDs for the session and each event. Repeating the same create or event request is idempotent; reusing an ID with different content returns a conflict. A session must reference an alarm version owned by the caller. Events are appended in a transaction and invalid transitions are rejected without persisting the event. Supported events include `AWAKE`, `MISSION_STARTED`, `MISSION_VALIDATED`, `MISSION_FAILED`, `FALLBACK`, `SNOOZE`, `COMPLETED`, `EMERGENCY_STOP`, and `CANCELLED`. Snooze events carry `{ "count": 1 }` through `{ "count": 20 }` in `payload`.

## Statistics

- `GET /api/v1/statistics/summary` — return the caller's lifetime wake totals, completion rate, average completion time, and current/best consecutive-day streak. Successful wake days use each alarm's fixed timezone or the caller's latest registered device timezone.

## Offline alarm sync and conflicts

- `POST /api/v1/sync` — atomically reconcile up to 100 queued `ALARM` operations

Each operation uses the mobile queue shape: `id`, `operationType`, `entityType`, `entityId`, optional `clientVersion`, `payload`, and `occurredAt`. Supported alarm operation types are `CREATE_ALARM`, `UPDATE_ALARM`, `DELETE_ALARM`, and `UPSERT`. `clientVersion` is the expected server version for updates/deletes; a missing alarm can be created at client version 0 or 1. Alarm payloads support the same fields as the CRUD API, including `qrExpectedCode`. Successful operations are applied in the same transaction as their idempotency record and alarm version snapshot.

The response keeps `acceptedIds` and `accepted` for applied or previously acknowledged operations and adds `conflicts`. A version mismatch returns the stored server alarm and version without changing the alarm. Replaying the same operation ID and content repeats its original applied/conflict result; reusing an ID with changed content returns HTTP 409. Unsupported entity types or operation types return HTTP 422. `occurredAt` accepts ISO dates, Unix milliseconds, Unix seconds, and Swift `Date` seconds since 2001. Existing intake records from before reconciliation remain acknowledged and are never replayed against alarm data.

## Analytics ingestion

- `POST /api/v1/analytics/events` — atomically accept up to 100 client events

Each event uses `id`, `eventName`, `eventVersion`, optional `properties`, and optional `occurredAt`. Event properties are limited to 16 KiB per event. IDs are idempotency keys, and reusing an ID with changed content returns a conflict. Analytics rows are scoped to the authenticated anonymous account and latest registered device.

## Support diagnostics

- `POST /api/v1/support/diagnostics` — create a support ticket with a limited diagnostics object

The caller supplies an idempotency UUID, category (`ALARM`, `MISSION`, `SYNC`, or `OTHER`), and optional app/platform/OS/timezone/alarm/error-code/pending-sync fields. Unknown fields and invalid timezones are rejected. The API stores at most 16 KiB and acknowledges retries with the same ticket contents.
