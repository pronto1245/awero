# AWERO

**AWERO — Wake up. Stay up.**

AI-powered behavioral alarm for iOS and Android.

## Architecture

AWERO is a **Local-First Mobile Product + Cloud Intelligence**.

The critical wake path never requires the backend, internet, AI or an active subscription:

`Alarm → Mission → Validation → Fallback → Wake Session`

## Monorepo

- `apps/api` — NestJS REST API
- `apps/worker` — background jobs
- `apps/admin` — admin panel
- `apps/ios` — SwiftUI application
- `apps/android` — Kotlin/Compose application
- `packages/api-contracts` — API contracts
- `packages/shared-types` — domain types
- `packages/localization` — six-language resources
- `packages/validation` — shared validation
- `database` — PostgreSQL migrations/seeds
- `infrastructure` — local/deployment infrastructure
- `docs` — product and technical documentation

## Development

Copy `.env.example` to `.env`, then:

```bash
docker compose -f infrastructure/docker/docker-compose.yml up -d
pnpm install
pnpm dev:api
```

## Product rule

Alarm reliability comes before AI. No remote feature flag may disable the local alarm, fallback or emergency stop.
