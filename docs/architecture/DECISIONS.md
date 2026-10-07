# Architecture Decisions

## ADR-001: Monorepo

One repository contains independently deployable backend, worker, admin, iOS and Android applications plus shared contracts.

## ADR-002: Local-first alarm

The device is authoritative for the next critical alarm. Backend is not part of the execution path.

## ADR-003: Modular monolith

MVP backend uses one NestJS application. Separate worker process is available for asynchronous jobs. Microservices are deferred.

## ADR-004: AI behind policy

AI returns structured recommendations only. Schema validation and a deterministic policy engine decide what may be applied.

## ADR-005: Anonymous-first

A user can create and use the first alarm without creating an account. Account conversion happens later without losing local data.
