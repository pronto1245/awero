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

Create requests require integer `hour` (0–23) and `minute` (0–59). Optional fields are `label`, `timezoneMode`, `fixedTimezone`, `snoozeEnabled`, `maxSnoozes`, `snoozeMinutes`, `missionType`, and `difficulty`. Unknown properties and invalid enum/timezone values are rejected.

All API routes use the single global `/api/v1` prefix. Apply migrations through `pnpm --filter @awero/api db:migrate` for an empty database or through the local Docker Compose startup. Production schema changes remain migration-only.
