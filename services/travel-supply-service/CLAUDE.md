# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build       # Build project
./gradlew test        # Run all tests
./gradlew bootRun     # Run locally on port 8082
./gradlew clean       # Clean build artifacts
```

Run a single test class:
```bash
./gradlew test --tests "com.sofly.supply.GooglePlacesTest"
```

Swagger UI is available at `http://localhost:8082/swagger-ui` when running.

## Architecture

Hexagonal Architecture (Ports & Adapters):

```
adapter/inbound/rest/        ← REST controllers (entrypoints)
adapter/outbound/amadeus/    ← Amadeus API client (auth, flights, hotels)
adapter/outbound/google/     ← Google Places API client
application/service/         ← Business logic
application/port/outbound/   ← Supplier port interfaces (FlightSupplierPort, HotelSupplierPort)
application/dto/             ← Data transfer objects
config/                      ← WebClient bean configs
bootstrap/                   ← SupplierRegistry (auto-discovers supplier implementations)
```

### Supplier System

New flight/hotel suppliers are added by implementing `FlightSupplierPort` or `HotelSupplierPort` and registering them. `SupplierRegistry` auto-discovers all port implementations at startup. `SupplierRouter` routes requests to the correct supplier by key (configurable default: `amadeus`).

### Amadeus Integration

- OAuth2 Client Credentials flow; tokens are cached in `TokenCache` with a 30-second expiry buffer.
- Hotel search is a two-step process: fetch hotel IDs by city (`/v1/reference-data/locations/hotels/by-city`), then fetch offers (`/v3/shopping/hotel-offers`). Max 20 hotels.
- Flight search hits `/v2/shopping/flight-offers` and returns raw `JsonNode`.
- WebClient is configured with a 10MB buffer (needed for large hotel lists) in `WebClientConfig`.

### Google Places Enrichment

`HotelSearchService` optionally calls `GooglePlacesClient` to enrich hotel results with ratings and review counts. This step fails gracefully (returns `null`) if `GOOGLE_PLACES_API_KEY` is not set. `GooglePlacesController` is only active on non-`prod` profiles (for testing).

## Configuration

Required environment variables:
- `AMADEUS_CLIENT_ID` / `AMADEUS_CLIENT_SECRET`
- `GOOGLE_PLACES_API_KEY` (optional — disables enrichment if absent)

Key `application.yaml` settings:
- `server.port: 8082`
- `amadeus.base-url`: defaults to `https://test.api.amadeus.com`
- `google.places.base-url`: defaults to `https://places.googleapis.com`
- `app.suppliers.default: amadeus`

## Reference Docs
- `docs/api-spec.md` - API endpoints, schemas, enums