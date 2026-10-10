# AWERO

**AWERO — Wake up. Stay up.**

Local-first alarm and wake-up app for iOS and Android. Optional AI features remain future scope.

## Architecture

AWERO is a **Local-First Mobile Product + Cloud Intelligence**.

The critical wake path never requires the backend, internet, AI or an active subscription:

`Alarm → Mission → Validation → Fallback → Wake Session`

## Monorepo

- `apps/api` — NestJS REST API
- `apps/ios` — SwiftUI application
- `apps/android` — Kotlin/Compose application
- `packages/api-contracts` — API constants foundation; canonical OpenAPI contract is not yet implemented
- `packages/shared-types` — shared TypeScript types foundation; mobile clients are not yet wired to it
- `packages/localization` — six-language JSON sources; native resource generation is not yet wired
- `packages/schemas` — AI schema policy documentation, not runtime validation
- `database` — PostgreSQL SQL migrations
- `infrastructure` — local/deployment infrastructure
- `docs` — product and technical documentation

The only implementation order is [the AWERO Product Specification and Implementation Plan](docs/AWERO_PRODUCT_SPEC_AND_IMPLEMENTATION_PLAN.md); project structure documentation does not define a separate roadmap.

## Development

Copy `.env.example` to `.env`, then:

```bash
docker compose -f infrastructure/docker/docker-compose.yml up -d
pnpm install
pnpm dev:api
```

## Product rule

Alarm reliability comes before AI. No remote feature flag may disable the local alarm, fallback or emergency stop.
