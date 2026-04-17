# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run locally (uses application-core.yaml)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.sofly.core.ClassName"

# Run a specific test method
./gradlew test --tests "com.sofly.core.ClassName.methodName"

# Build JAR (skips tests, used by Docker)
./gradlew clean bootJar -x test
```

## Environment

The app uses `me.paulschwarz:spring-dotenv` to load a `.env` file from the project root. Required variables:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — PostgreSQL
- `REDIS_HOST`, `REDIS_PORT` — Redis
- `JWT_SECRET_KEY`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`
- `GEMINI_API_KEY` — Google Gemini AI
- `AWS_S3_ACCESS_KEY_ID`, `AWS_S3_SECRET_ACCESS_KEY`, `S3_BUCKET`, `AWS_REGION` — S3 (album)
- `OAUTH2_REDIRECT_URI` — OAuth2 success redirect (default: `http://localhost:3000/oauth2/callback`)
- `SUPPLY_SERVICE_URL` — supply service base URL (default: `http://supply:8081`)

Config file: `src/main/resources/application-core.yaml` (not `application.yaml`). The `bootRun` task and Docker entry point both pass `-Dspring.config.name=application-core`.

Swagger UI: `http://localhost:8080/core/swagger-ui`

## Architecture

### Package Structure

```
com.sofly.core
├── global/          # Cross-cutting concerns
│   ├── ai/          # Spring AI integration
│   │   ├── config/  # ChatClientConfig (Gemini 2.5 Flash)
│   │   ├── memory/  # RdbChatMemory (PostgreSQL + Redis hybrid)
│   │   └── prompt/  # SystemPrompts (travel planner prompt)
│   ├── auth/        # AuthController + AuthService (token refresh)
│   ├── config/      # RedisConfig, S3Config
│   ├── entity/      # BaseTimeEntity (@MappedSuperclass for createdAt/updatedAt)
│   ├── exception/   # ErrorCode, GlobalExceptionHandler, SoflyException
│   ├── response/    # ApiResponse + BaseErrorCode/BaseSuccessCode/ErrorStatus/SuccessStatus
│   └── security/    # SecurityConfig, JWT filter/provider, OAuth2 handlers
│       └── workspace/ # @RequireWorkspaceMember + WorkspaceMemberAspect (AOP)
└── domain/          # Business domains
    ├── user/        # User entity (OAuth2-backed), UserController (profile CRUD)
    ├── workspace/   # Workspace, WorkspaceMember, SavedFlight
    │   # Controllers split: WorkspaceController, WorkspaceFlightController,
    │   #                    WorkspaceInviteController, WorkspaceMemberController
    ├── schedule/    # Schedule + ScheduleItem, full CRUD API
    ├── chat/        # ChatRoom + ChatMessage, AI-powered travel planner chat
    ├── album/       # Album + Photo (AWS S3 storage)
    ├── travellog/   # TravelLog (markdown) + TravellogPhotoController (photo linking)
    └── conquest/    # 정복지도 — VisitedCountry, VisitedCity, AirportInfoService
```

### Auth Flow

- Sessions are **stateless** (`SessionCreationPolicy.STATELESS`).
- Social login (Google / Kakao / Naver) via Spring OAuth2 Client → `CustomOAuth2UserService` creates/updates the `User` entity → `OAuth2AuthenticationSuccessHandler` issues JWT pair and redirects to `OAUTH2_REDIRECT_URI`.
- All subsequent requests carry a JWT Bearer token, validated by `JwtAuthenticationFilter`.
- Refresh tokens are stored in Redis (`RefreshTokenRepository`). Token refresh endpoint: `POST /api/auth/refresh`.

### Workspace Access Control

`@RequireWorkspaceMember(minRole = MemberRole.EDITOR)` is an AOP annotation that validates workspace membership before the method executes. The annotated method **must** have a `Long workspaceId` parameter. Roles are checked by enum ordinal: `OWNER(0) > EDITOR(1) > VIEWER(2)`.

### Domain Model Notes

- All entities extend `BaseTimeEntity`; `@EnableJpaAuditing` is on the main application class.
- `Workspace` is the root aggregate. `WorkspaceMember` is the join table between `Workspace` and `User` with roles (OWNER / EDITOR / VIEWER).
- `Schedule` supports multiple versions per workspace. `ScheduleItem` has `orderIndex` and `Category` (ACCOMMODATION, RESTAURANT, CAFE, ATTRACTION, TRANSPORT).
- `ChatRoom` is scoped to a workspace (one per workspace). `ChatMessage` stores USER/ASSISTANT messages.
- `SavedFlight` stores denormalized flight info linked to a workspace. Saving a flight fires `FlightSavedEvent`, which triggers `ConquestMapService.onFlightSaved()`.
- `Album` is created lazily per workspace. `Photo` stores S3 key + object URL. Upload allows jpeg/png/webp/heic, max 10MB per file, 20 files per request. Delete requires OWNER/EDITOR role or being the uploader.
- `TravelLog` is a markdown journal with visibility (PRIVATE, MEMBERS, PUBLIC). Photos can be linked via `TravellogPhotoController`.
- All JPA relationships use `FetchType.LAZY`.

### Conquest Map (정복지도)

`domain/conquest` tracks which countries and cities a user has visited.

- **`VisitStatus`**: `UNVISITED → PLANNED → VISITED`
- **Auto-PLANNED**: When a flight is saved to a workspace, `FlightSavedEvent` is published. `ConquestMapService.onFlightSaved()` resolves the arrival airport via `AirportInfoService` and marks the destination country/city as `PLANNED` for all workspace members.
- **Auto-VISITED**: `VisitStatusScheduler` runs periodically and promotes `PLANNED` entries to `VISITED` once the flight's departure time has passed.
- **Manual override**: Country/city status can be set manually via the API. Bulk import is supported.
- `AirportInfoService` holds a static airport-to-country/city/coordinate mapping (IATA code lookup). It also exposes `calculateDistanceKm()` used in stats (total distance traveled).
- `ConquestMapController` exposes: conquest map, stats panel, trip routes (arcs between airports), country-scoped workspaces, and CRUD for country/city status.

### AI Chat (Travel Planner)

- **Model:** Google Gemini 2.5 Flash via Spring AI (`spring.ai.google.genai`)
- **Memory:** `RdbChatMemory` uses PostgreSQL as primary store and Redis (24-hour TTL) as cache. Falls back to DB on Redis miss. Sliding window of 20 messages. Conversation ID: `"room:{chatRoomId}"`.
- **System Prompt:** `SystemPrompts.TRAVEL_PLANNER` runs a 3-stage conversation: (1) information gathering, (2) natural language itinerary proposal, (3) JSON output only when user explicitly confirms (e.g., "확정해줘", "저장해줘").

### Key Infrastructure

- **OpenFeign** (`@EnableFeignClients`) is enabled for future inter-service calls. The supply service URL is configured via `sofly.supply.url`.
- **`@ConfigurationPropertiesScan`** is on the main class.
- Spring Cloud version: `2025.0.0`.
