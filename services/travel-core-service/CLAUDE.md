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
- `FRONTEND_REDIRECT_URL`
- `GEMINI_API_KEY` — Google Gemini AI API key

Config file: `src/main/resources/application-core.yaml` (not `application.yaml`). The `bootRun` task and the Docker entry point both pass `-Dspring.config.name=application-core`.

Swagger UI is available at `http://localhost:8080/core-docs`.

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
│   ├── config/      # RedisConfig
│   ├── entity/      # BaseTimeEntity (@MappedSuperclass for createdAt/updatedAt)
│   ├── exception/   # ErrorCode, GlobalExceptionHandler, SoflyException
│   ├── response/    # ApiResponse (common API response wrapper)
│   └── security/    # SecurityConfig, JWT filter/provider, OAuth2 handlers
└── domain/          # Business domains
    ├── user/        # User entity (OAuth2-backed), UserRepository, UserService
    ├── workspace/   # Workspace (trip container), WorkspaceMember, SavedFlight
    ├── schedule/    # Schedule + ScheduleItem, full CRUD API
    ├── chat/        # ChatRoom + ChatMessage, AI-powered travel planner chat
    ├── album/       # Album + Photo (Google Drive metadata)
    └── travellog/   # TravelLog (markdown post-trip diary)
```

### Auth Flow

- Sessions are **stateless** (`SessionCreationPolicy.STATELESS`).
- Social login (Google / Kakao / Naver) via Spring OAuth2 Client → `CustomOAuth2UserService` creates/updates the `User` entity → `OAuth2AuthenticationSuccessHandler` issues JWT pair and redirects to `FRONTEND_REDIRECT_URL`.
- All subsequent requests carry a JWT Bearer token, validated by `JwtAuthenticationFilter`.
- Refresh tokens are stored in Redis (`RefreshTokenRepository`). Token refresh endpoint: `POST /api/auth/refresh`.

### Domain Model Notes

- All entities extend `BaseTimeEntity` for audit timestamps; `@EnableJpaAuditing` is on the main application class.
- `Workspace` is the root aggregate for a trip. `WorkspaceMember` is the join table between `Workspace` and `User`.
- `Schedule` supports multiple versions per workspace. `ScheduleItem` represents a draggable place/activity card with `orderIndex` and `Category` (ACCOMMODATION, RESTAURANT, CAFE, ATTRACTION, TRANSPORT).
- `ChatRoom` is scoped to a workspace (one per workspace). `ChatMessage` stores USER/ASSISTANT messages for AI chat history.
- `SavedFlight` stores denormalized flight info (from supply service) linked to a workspace.
- `Album` has a one-to-one relationship with `Workspace` and holds a Google Drive folder ID. `Photo` stores thumbnail/original URLs and EXIF metadata.
- `TravelLog` is a markdown-based travel journal with visibility control (PRIVATE, MEMBERS, PUBLIC).
- All JPA relationships use `FetchType.LAZY`.

### AI Chat (Travel Planner)

- **Model:** Google Gemini 2.5 Flash via Spring AI (`spring.ai.google.genai`)
- **Memory:** `RdbChatMemory` implements Spring AI's `ChatMemory` interface using a hybrid strategy — PostgreSQL as primary store and Redis (24-hour TTL) as cache. Falls back to DB on Redis miss. Sliding window of 20 recent messages. Conversation ID format: `"room:{chatRoomId}"`.
- **Flow:** `ChatService` uses `ChatClient` with `MessageChatMemoryAdvisor`. User messages and assistant replies are persisted via `RdbChatMemory`.
- **System Prompt:** `SystemPrompts.TRAVEL_PLANNER` guides a 3-stage conversation: (1) information collection, (2) itinerary proposal in natural language, (3) JSON output only when user explicitly confirms (e.g., "확정해줘", "저장해줘").

### Key Infrastructure

- **OpenFeign** (`@EnableFeignClients`) is enabled for future inter-service calls.
- **`@ConfigurationPropertiesScan`** is on the main class; add `@ConfigurationProperties` beans freely.
- Spring Cloud version: `2025.0.0`.
