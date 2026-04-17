# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build       # Build project
./gradlew test        # Run all tests
./gradlew bootRun     # Run locally on port 8081
./gradlew clean       # Clean build artifacts
```

Run a single test class:
```bash
./gradlew test --tests "com.sofly.supply.GooglePlacesTest"
```

Swagger UI is available at `http://localhost:8081/supply/swagger-ui` when running.

## Architecture

Hexagonal Architecture (Ports & Adapters):

```
adapter/inbound/rest/          ← REST controllers (entrypoints)
adapter/outbound/google/       ← Google Places API client
adapter/outbound/rapidapi/
    flights/                   ← BookingComFlightSupplierAdapter, BookingComFlightMetaClient
    hotels/                    ← BookingComHotelAdapter, BookingComHotelMetaClient
application/service/           ← Business logic
application/port/outbound/     ← Port interfaces: FlightSupplierPort, HotelSupplierPort,
                               #                  FlightMetaPort, HotelMetaPort, PlaceInfoPort
application/dto/               ← Request/Response DTOs
config/                        ← WebClient bean configs, RedisCacheConfig
bootstrap/                     ← SupplierRegistry (auto-discovers supplier implementations)
```

### Supplier System

`SupplierRegistry` auto-discovers all `FlightSupplierPort` and `HotelSupplierPort` implementations at startup and maintains separate supplier maps. `SupplierRouter` routes requests by key via `selectFlightSupplier(key)` / `selectHotelSupplier(key)`.

**Default supplier: `booking`** (Booking.com via RapidAPI — previously `amadeus`, Amadeus has been removed).

Both `/supply/flights/offers` and `/supply/hotels/offers` accept an optional `supplier` query param for runtime supplier selection.

### Booking.com (RapidAPI) Integration

Both flight and hotel search use `rapidApiWebClient` (from `WebClientConfig`), which injects `X-RapidAPI-Key` and `X-RapidAPI-Host` headers from `RapidApiProperties`.

- **Flights** (`BookingComFlightSupplierAdapter`, key: `"booking"`): hits `/api/v1/flights/searchFlights`. Uses `BookingComFlightMetaClient` for airport/destination ID lookup. `BookingComFlightResponseFilter` post-processes the raw response. `FlightSearchRequest` contains Booking.com-specific fields: `stops` (StopsType), `sort` (SortType), `cabinClass` (CabinClass), `childrenAge`, `pageNo`.
- **Hotels** (`BookingComHotelAdapter`, key: `"booking"`): hits `/api/v1/hotels/searchHotels`. Uses `BookingComHotelMetaClient` for destination ID lookup. Results are cached in Redis (`@Cacheable(value = "bookingHotels")`). `HotelSearchRequest` fields include `destId`, `searchType`, `arrivalDate`, `departureDate`, `sortBy`, `priceMin`/`priceMax`, `roomQty`, `pageNumber`, `categoriesFilter`, etc.

### Google Places Integration

`GooglePlacesClient` provides two operations:
- `searchText(text)` — text search returning `PlacesResponse` (displayName, formattedAddress, priceLevel, photos list)
- `getPhotoMedia(name, maxWidthPx)` — fetches actual image URL (`photoUri`) from a `photos[].name` value in a `PlacesResponse`

`PlaceInfoPort` exposes these for use in hotel enrichment. Fails gracefully (returns `Optional.empty()`) if `GOOGLE_PLACES_API_KEY` is not set.

`GooglePlacesController` (`@Profile("!prod")`) exposes:
- `GET /supply/places?text=...` — place search
- `GET /supply/places/photo?name=...&maxWidthPx=...` — photo URL lookup

### Redis Caching

`RedisCacheConfig` sets up Spring Cache with Redis. Currently active caches:
- `bookingHotels` — hotel search results keyed by `HotelSearchRequest`

## Configuration

Required environment variables:
- `RAPIDAPI_KEY` / `RAPIDAPI_HOST` — Booking.com RapidAPI credentials
- `GOOGLE_PLACES_API_KEY` (optional — disables Places enrichment if absent)
- `REDIS_HOST`, `REDIS_PORT` — Redis for caching

Key `application.yaml` settings:
- `server.port: 8081`
- `app.suppliers.default: booking`
- `rapidapi.base-url`: Booking.com RapidAPI base URL
- `rapidapi.api-key` / `rapidapi.host`: RapidAPI credentials
- `google.places.base-url`: defaults to `https://places.googleapis.com`
