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

## ADR-006: Native iOS alarm delivery

On iOS 26 and later, device-local repeating alarms, test alarms, and snooze alarms use AlarmKit so the operating system presents and sounds the alarm even when the app is not active. AlarmKit authorization is required and scheduling errors must be shown to the user; a saved alarm must never silently appear scheduled when the system rejected it.

The existing UserNotifications scheduler remains the compatibility path for iOS 17–25 and for fixed-timezone alarms that cannot be represented by AlarmKit's device-local weekly recurrence. This fallback is a notification, not an AlarmKit alarm, and must be described accurately in release validation.
