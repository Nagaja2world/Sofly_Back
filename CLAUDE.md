# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo Layout

This is a multi-module Gradle mono-repo with two independently deployable Spring Boot services:

| Service | Port (local) | Config file | Package root |
|---|---|---|---|
| `travel-core-service` | 8080 | `application-core.yaml` | `com.sofly.core` |
| `travel-supply-service` | 8082 | `application.yaml` | `com.sofly.supply` |

Each service has its own `CLAUDE.md` with detailed architecture notes — read those when working inside a specific service.

## Local Development

Start PostgreSQL and Redis before running either service:

```bash
docker compose -f docker-compose.local.yml up -d
```

Run a service:

```bash
./gradlew :services:travel-core-service:bootRun
./gradlew :services:travel-supply-service:bootRun
```

Build JARs (used by Docker, skips tests):

```bash
./gradlew :services:travel-core-service:clean bootJar -x test
./gradlew :services:travel-supply-service:clean bootJar -x test
```

Run all tests:

```bash
./gradlew test
# or per-service:
./gradlew :services:travel-core-service:test
./gradlew :services:travel-supply-service:test
```

Run a single test:

```bash
# core
./gradlew :services:travel-core-service:test --tests "com.sofly.core.ClassName.methodName"
# supply
./gradlew :services:travel-supply-service:test --tests "com.sofly.supply.ClassName.methodName"
```

## Architecture Overview

**travel-core-service** — layered architecture (domain-centric).  
Handles auth, workspace/trip management, AI-powered travel planner chat, schedules, albums, and travel logs.  
See `services/travel-core-service/CLAUDE.md` for domain model, auth flow, and AI chat details.

**travel-supply-service** — Hexagonal Architecture (Ports & Adapters).  
Handles external supplier integrations: Google Places API for place search/photos, Amadeus for flights and hotels, Booking.com via RapidAPI.  
`SupplierRegistry` auto-discovers `FlightSupplierPort` / `HotelSupplierPort` implementations at startup. A `supplier=<key>` query param selects the supplier at runtime.  
See `services/travel-supply-service/CLAUDE.md` for supplier system and integration details.

**Inter-service communication:** OpenFeign is enabled on the core service (`@EnableFeignClients`) but not yet actively used — services are currently independent.

## Deployment

Production uses `docker-compose.yml` with blue-green deployments via Nginx (see `docs/nginx-bluegreen.md`). Each service is deployed with a Docker profile (`core` or `supply`) and reads env vars from `.env.core` / `.env.supply` on the host.

GitHub Actions workflows in `.github/workflows/` handle CI/CD for each service independently.

## Git Convention

Branch format: `feat/이슈번호-기능명`, `fix/이슈번호-버그명`, `hotfix/이슈번호-버그명`  
Base branches: `main` (production), `develop` (integration)

Commit format: `type(scope): subject`  
Types: `feat`, `fix`, `refactor`, `chore`, `docs`, `style`, `test`, `ci`, `perf`
