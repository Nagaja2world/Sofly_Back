# Sofly Travel Core Service API Specification

> Version: 2026-05-08  
> Base URL: `http://localhost:8080`  
> Content-Type: `application/json`  
> Auth: `Authorization: Bearer {accessToken}`

---

## 1. 문서 개요

이 문서는 `travel-core-service`가 제공하는 전체 API를 기술문서 삽입용으로 정리한 통합 명세서입니다.

| 구분 | 설명 |
|---|---|
| API명 | 사용자가 수행하는 기능 중심의 API 이름 |
| 역할/요약 | API가 처리하는 핵심 업무 |
| 엔드포인트 | 호출 URL. `{id}`는 Path Variable |
| HTTP 메서드 | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` |
| Request | Path/Query/Body/Form-data 요구사항 |
| Response | 주요 응답 데이터. 공통 래핑 응답은 별도 표기 |
| 상태코드 | 성공 및 예외 처리 코드 |
| 인증/인가 | 호출 시 필요한 Access Token, 워크스페이스 권한, 소유자 권한 등 |

---

## 2. 공통 규칙

### 2.1 인증 헤더

```http
Authorization: Bearer eyJhbGci...
```

| 범위 | 인증 요구사항 |
|---|---|
| 기본 정책 | `/api/auth/refresh`, `/api/v1/messaging/**`, `/ws/**`, OAuth2 로그인, Swagger, Actuator를 제외한 대부분의 API는 JWT 인증 필요 |
| 워크스페이스 API | JWT 인증 + 워크스페이스 멤버 권한 필요 |
| 소유자 전용 API | JWT 인증 + 워크스페이스 `OWNER` 권한 필요 |
| WebSocket | HTTP handshake는 허용되지만 STOMP `CONNECT`, `SEND`, `SUBSCRIBE` 시 `Authorization` native header 사용 |

### 2.2 공통 응답 포맷

일부 Schedule/Chat API는 아직 공통 래퍼 없이 DTO를 직접 반환합니다. 표에서 `ApiResponse<T>`로 표시된 API는 아래 포맷을 따릅니다.

```json
{
  "success": true,
  "code": null,
  "message": null,
  "data": {}
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `success` | boolean | 요청 성공 여부 |
| `code` | string | 실패 코드. 일부 도메인 예외는 메시지만 반환될 수 있음 |
| `message` | string | 성공/실패 메시지 |
| `data` | object/array/null | 실제 응답 데이터 |

### 2.3 공통 상태코드

| HTTP | 의미 | 대표 상황 |
|---|---|---|
| `200 OK` | 조회/수정/처리 성공 | 일반 성공 |
| `201 Created` | 리소스 생성 성공 | 워크스페이스, 여행기, 정복 도시 생성 |
| `204 No Content` | 본문 없는 처리 성공 | 삭제, 이동, 일괄 등록 |
| `400 Bad Request` | 요청 값 오류 | Validation 실패, 잘못된 초대 코드 |
| `401 Unauthorized` | 인증 실패 | Access Token 없음/만료/위조 |
| `403 Forbidden` | 인가 실패 | 워크스페이스 접근 권한 없음, 소유자 아님 |
| `404 Not Found` | 리소스 없음 | 사용자/워크스페이스/일정/사진/채팅방 없음 |
| `409 Conflict` | 상태 충돌 | 이미 워크스페이스 멤버 |
| `422 Unprocessable Entity` | 처리 불가 | AI 응답을 일정으로 변환할 수 없음 |
| `502 Bad Gateway` | 외부 공급 서비스 오류 | Supply API 호출 실패 |
| `500 Internal Server Error` | 서버 내부 오류 | 예상하지 못한 예외 |

### 2.4 주요 Enum

| Enum | 값 |
|---|---|
| `AgeGroup` | `TEENS`, `TWENTIES`, `THIRTIES`, `FORTIES_PLUS` |
| `CompanionType` | `SOLO`, `COUPLE`, `FRIENDS`, `FAMILY` |
| `TravelTheme` | `RELAXATION`, `SHOPPING`, `CULTURE`, `ACTIVITY`, `FOOD` |
| `MemberRole` | `OWNER`, `EDITOR`, `VIEWER` |
| `FlightType` | `OUTBOUND`, `RETURN` |
| `VisitStatus` | `UNVISITED`, `PLANNED`, `VISITED` |
| `Continent` | `ASIA`, `EUROPE`, `NORTH_AMERICA`, `SOUTH_AMERICA`, `AFRICA`, `OCEANIA` |
| `ScheduleItem.Category` | 일정 아이템 도메인 enum. 코드 기준으로 `category` 필드는 필수 |
| `ChatMessage.Role` | AI 채팅 메시지 역할 enum |
| `ChatRoomType` | 실시간 메시징 방 타입 enum |
| `ChatMessageType` | 실시간 메시징 메시지 타입 enum |

---

## 3. API Index

| Domain | API 수 | 주요 기능 |
|---|---:|---|
| Auth | 2 + OAuth2 | 토큰 재발급, 로그아웃, 소셜 로그인 |
| User Profile | 2 | 내 프로필 조회/수정 |
| Flight | 3 | 항공권 검색, 공항 검색, 항공편 상세 |
| Hotel | 4 | 호텔 검색, 목적지/정렬/필터 옵션 |
| Place | 2 | 장소 검색, 장소 사진 조회 |
| Workspace | 12 | 여행 워크스페이스 생성/조회/수정/삭제, 초대, 멤버, 항공편 저장 |
| Schedule | 12 | 일정 생성/조회/복사/수정/삭제, 아이템 관리, 지도 핀 |
| AI Chat | 6 | AI 채팅방, 메시지, 스트리밍, 일정 저장 |
| Messaging | 3 REST + 1 WS | 실시간 채팅방/메시지 |
| Conquest Map | 9 | 방문 국가/도시, 통계, 항공 경로 |
| Album | 4 | 워크스페이스 앨범/사진 |
| TravelLog | 8 | 여행기 CRUD, 사진 첨부 |
| Workspace SNS | Planned | 피드/댓글/좋아요/공유 예정 |

---

## 4. Auth API

### OAuth2 소셜 로그인

| 항목 | 내용 |
|---|---|
| API명 | 소셜 로그인 시작 |
| 역할/요약 | Spring Security OAuth2 인증 플로우 시작 |
| 엔드포인트 | `/oauth2/authorization/{provider}` |
| HTTP 메서드 | `GET` |
| Request | `provider`: `google`, `kakao`, `naver` |
| Response | 성공 시 프론트 콜백 URL로 `accessToken`, `refreshToken` 쿼리 전달 |
| 상태코드 | `302 Redirect`, 실패 시 콜백에 `error` 쿼리 |
| 인증/인가 | 불필요 |

### Auth 명세

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 토큰 재발급 | Refresh Token으로 Access/Refresh Token 재발급 | `/api/auth/refresh` | `POST` | Body: `RefreshTokenRequest` | `ApiResponse<TokenResponse>` | `200`, `400`, `401` | 불필요. 유효한 Refresh Token 필요 |
| 로그아웃 | 현재 사용자 Refresh Token 삭제 | `/api/auth/logout` | `POST` | 없음 | `ApiResponse<Void>` | `200`, `401` | JWT 필요 |

**RefreshTokenRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `refreshToken` | string | Y | Refresh Token |

**TokenResponse**

| 필드 | 타입 | 설명 |
|---|---|---|
| `accessToken` | string | 새 Access Token |
| `refreshToken` | string | 새 Refresh Token |

---

## 5. User Profile API

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 프로필 조회 | 현재 로그인한 사용자 프로필 조회 | `/api/users/me/profile` | `GET` | 없음 | `ApiResponse<UserProfileResponse>` | `200`, `401`, `404` | JWT 필요 |
| 프로필 등록/수정 | 여행 성향 및 기본 프로필 부분 수정 | `/api/users/me/profile` | `PATCH` | Body: `UserProfileUpdateRequest` | `ApiResponse<UserProfileResponse>` | `200`, `400`, `401`, `404` | JWT 필요 |

**UserProfileUpdateRequest**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `nickname` | string | N | 최대 20자 |
| `city` | string | N | 거주 도시 |
| `ageGroup` | enum | N | `AgeGroup` |
| `preferCompanionType` | enum | N | `CompanionType` |
| `budgetMin` | integer | N | 0 이상 |
| `budgetMax` | integer | N | 0 이상, `budgetMin` 이상 권장 |
| `preferThemes` | array enum | N | 최대 5개 |
| `preferCities` | array string | N | 최대 10개 |

**UserProfileResponse**

| 필드 | 타입 | 설명 |
|---|---|---|
| `id`, `email`, `nickname`, `profileImageUrl` | number/string | 사용자 기본 정보 |
| `ageGroup`, `city`, `preferCompanionType` | enum/string | 여행 성향 |
| `budgetMin`, `budgetMax` | integer | 선호 예산 범위 |
| `preferThemes`, `preferCities` | array | 선호 테마/도시 |
| `profileCompleted` | boolean | AI 일정 생성에 필요한 핵심 프로필 완성 여부 |

---

## 6. Flight API

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 항공편 검색 | 공급자 API를 통해 항공권 오퍼 검색 | `/api/v1/flights/offers` | `GET` | Query: `supplier`, `FlightSearchRequest` | `ApiResponse<JsonNode>` | `200`, `400`, `401`, `502` | JWT 필요 |
| 공항 검색 | 항공 검색용 출발/도착 공항 ID 조회 | `/api/v1/flights/destinations` | `GET` | Query: `query`, `languageCode` | `ApiResponse<List<FlightDestination>>` | `200`, `400`, `401`, `502` | JWT 필요 |
| 항공편 상세 조회 | 검색 응답의 token으로 상세 정보 조회 | `/api/v1/flights/details` | `GET` | Query: `supplier`, `token`, `currencyCode` | `ApiResponse<JsonNode>` | `200`, `400`, `401`, `502` | JWT 필요 |

**FlightSearchRequest Query**

| 필드 | 타입 | 설명 |
|---|---|---|
| `fromId`, `toId` | string | 예: `ICN.AIRPORT`, `CEB.AIRPORT` |
| `departDate`, `returnDate` | date | `yyyy-MM-dd` |
| `adults` | integer | 기본값 2 |
| `currencyCode` | string | 예: `KRW` |
| `stops` | enum | `none`, `ZERO`, `ONE`, `TWO` |
| `sort` | enum | `BEST`, `CHEAPEST`, `FASTEST` |
| `cabinClass` | enum | `ECONOMY`, `PREMIUM_ECONOMY`, `BUSINESS`, `FIRST` |
| `childrenAge` | string | 아동 나이 조건 |
| `pageNo` | integer | 기본값 1 |
| `airlines` | array string | 항공사 코드 필터 |
| `cursor` | string | 다음 페이지 커서 |

---

## 7. Hotel API

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 호텔 검색 | 조건에 맞는 호텔 오퍼 검색 | `/api/v1/hotels/offers` | `GET` | Query: `supplier`, `HotelSearchRequest` | `ApiResponse<JsonNode>` | `200`, `400`, `401`, `502` | JWT 필요 |
| 호텔 목적지 자동완성 | 호텔 검색 목적지 ID 조회 | `/api/v1/hotels/destinations` | `GET` | Query: `query` | `ApiResponse<List<HotelDestination>>` | `200`, `400`, `401`, `502` | JWT 필요 |
| 호텔 정렬 옵션 조회 | 검색 조건에 사용 가능한 정렬 옵션 조회 | `/api/v1/hotels/sort-options` | `GET` | Query: `HotelOptionsRequest` | `ApiResponse<List<HotelSortOption>>` | `200`, `400`, `401`, `502` | JWT 필요 |
| 호텔 필터 옵션 조회 | 검색 조건에 사용 가능한 필터 옵션 조회 | `/api/v1/hotels/filter-options` | `GET` | Query: `HotelOptionsRequest` | `ApiResponse<JsonNode>` | `200`, `400`, `401`, `502` | JWT 필요 |

**HotelSearchRequest Query**

| 필드 | 타입 | 설명 |
|---|---|---|
| `destId`, `searchType` | string | 목적지 ID/타입 |
| `arrivalDate`, `departureDate` | date | `yyyy-MM-dd` |
| `adults`, `roomQty`, `pageNumber` | integer | 투숙 인원/객실/페이지 |
| `childrenAge` | string | 아동 나이 조건 |
| `priceMin`, `priceMax` | number | 가격 범위 |
| `sortBy`, `categoriesFilter` | string | 정렬/카테고리 필터 |
| `units` | enum | `METRIC`, `IMPERIAL` |
| `temperatureUnit` | enum | `CELSIUS`, `FAHRENHEIT` |
| `languageCode`, `currencyCode`, `location` | string | 언어/통화/지역 |

**HotelOptionsRequest Query**

| 필드 | 타입 | 설명 |
|---|---|---|
| `destId`, `searchType` | string | 목적지 ID/타입 |
| `arrivalDate`, `departureDate` | date | 투숙 기간 |
| `adults`, `roomQty` | integer | 인원/객실 수 |
| `childrenAge`, `categoriesFilter` | string | 아동/카테고리 조건 |

---

## 8. Place API

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 장소 검색 | Google Places 기반 장소 검색 | `/api/v1/places` | `GET` | Query: `text` | `ApiResponse<PlacesResponse>` | `200`, `400`, `401`, `502` | JWT 필요 |
| 장소 사진 조회 | Places photo name으로 이미지 URI 조회 | `/api/v1/places/photo` | `GET` | Query: `name`, `maxWidthPx=800` | `ApiResponse<PhotoMedia>` | `200`, `400`, `401`, `502` | JWT 필요 |

**PlacesResponse.Place 주요 필드**

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | 장소 ID |
| `displayName.text` | string | 장소명 |
| `primaryType`, `formattedAddress` | string | 장소 유형/주소 |
| `location.latitude`, `location.longitude` | number | 좌표 |
| `rating`, `userRatingCount` | number | 평점/리뷰 수 |
| `googleMapsUri`, `websiteUri`, `nationalPhoneNumber` | string | 외부 링크/연락처 |
| `photos` | array | 사진 메타데이터 |

---

## 9. Workspace API

### 9.1 워크스페이스 기본

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 워크스페이스 생성 | 새 여행 워크스페이스 생성 | `/api/workspaces` | `POST` | Body: `CreateWorkspaceRequest` | `ApiResponse<WorkspaceResponse>` | `201`, `400`, `401` | JWT 필요. 생성자가 `OWNER` |
| 내 워크스페이스 목록 조회 | 소유/참여 중인 워크스페이스 목록 조회 | `/api/workspaces` | `GET` | 없음 | `ApiResponse<List<WorkspaceResponse>>` | `200`, `401` | JWT 필요 |
| 워크스페이스 상세 조회 | 워크스페이스 단건 조회 | `/api/workspaces/{workspaceId}` | `GET` | Path: `workspaceId` | `ApiResponse<WorkspaceResponse>` | `200`, `401`, `403`, `404` | JWT + 멤버 |
| 워크스페이스 수정 | 제목/목적지/기간 등 수정 | `/api/workspaces/{workspaceId}` | `PUT` | Body: `UpdateWorkspaceRequest` | `ApiResponse<WorkspaceResponse>` | `200`, `400`, `401`, `403`, `404` | JWT + `OWNER` |
| 워크스페이스 삭제 | 워크스페이스 삭제 | `/api/workspaces/{workspaceId}` | `DELETE` | Path: `workspaceId` | 없음 | `204`, `401`, `403`, `404` | JWT + `OWNER` |

**CreateWorkspaceRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | string | Y | 워크스페이스 제목 |
| `destination` | string | Y | 목적지 |
| `countryCode` | string | N | ISO alpha-2 국가 코드 |
| `startDate`, `endDate` | date | Y | 여행 기간 |
| `headcount` | integer | N | 인원 수 |
| `coverImageUrl` | string | N | 커버 이미지 |

**UpdateWorkspaceRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title`, `destination` | string | Y | 제목/목적지 |
| `startDate`, `endDate` | date | Y | 여행 기간 |
| `headcount` | integer | N | 인원 수 |
| `coverImageUrl` | string | N | 커버 이미지 |

**WorkspaceResponse**

| 필드 | 타입 | 설명 |
|---|---|---|
| `id`, `title`, `destination`, `countryCode` | number/string | 워크스페이스 기본 정보 |
| `startDate`, `endDate` | date | 여행 기간 |
| `headcount`, `coverImageUrl` | integer/string | 인원/커버 |
| `ownerId`, `memberCount` | number | 소유자/멤버 수 |

### 9.2 초대/참여

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 초대 코드 생성 | 워크스페이스 초대 링크 생성 | `/api/workspaces/{workspaceId}/invite-code` | `POST` | Path: `workspaceId` | `ApiResponse<InviteCodeResponse>` | `200`, `401`, `403`, `404` | JWT + `OWNER` |
| 초대 코드로 참여 | 초대 코드로 워크스페이스 멤버 참여 | `/api/workspaces/join/{inviteCode}` | `POST` | Path: `inviteCode` | `ApiResponse<WorkspaceResponse>` | `200`, `400`, `401`, `409` | JWT 필요 |

### 9.3 멤버 관리

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 멤버 목록 조회 | 워크스페이스 멤버 목록 조회 | `/api/workspaces/{workspaceId}/members` | `GET` | Path: `workspaceId` | `ApiResponse<List<WorkspaceMemberResponse>>` | `200`, `401`, `403`, `404` | JWT + 멤버 |
| 멤버 역할 변경 | 멤버의 `EDITOR`/`VIEWER` 역할 변경 | `/api/workspaces/{workspaceId}/members/{memberId}/role` | `PATCH` | Body: `UpdateMemberRoleRequest` | `ApiResponse<WorkspaceMemberResponse>` | `200`, `400`, `401`, `403`, `404` | JWT + `OWNER` |
| 멤버 내보내기/탈퇴 | 멤버 제거 또는 본인 탈퇴 | `/api/workspaces/{workspaceId}/members/{memberId}` | `DELETE` | Path: `workspaceId`, `memberId` | 없음 | `204`, `400`, `401`, `403`, `404` | JWT + `OWNER` 또는 본인 |

**UpdateMemberRoleRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `role` | enum | Y | `EDITOR`, `VIEWER`. `OWNER` 직접 지정 불가 |

### 9.4 워크스페이스 항공편

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 항공편 저장 | 검색한 항공편을 워크스페이스에 저장 | `/api/workspaces/{workspaceId}/flights` | `POST` | Body: `SaveFlightRequest` | `ApiResponse<SavedFlightResponse>` | `201`, `400`, `401`, `403`, `404` | JWT + 멤버 |
| 저장 항공편 목록 조회 | 워크스페이스 저장 항공편 조회 | `/api/workspaces/{workspaceId}/flights` | `GET` | Path: `workspaceId` | `ApiResponse<List<SavedFlightResponse>>` | `200`, `401`, `403`, `404` | JWT + 멤버 |

**SaveFlightRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `flightNumber`, `airline` | string | Y | 편명/항공사 |
| `departureAirport`, `arrivalAirport` | string | Y | 출발/도착 공항 |
| `departureTime`, `arrivalTime` | zoned datetime | Y | 출발/도착 시각 |
| `duration`, `price` | string/integer | N | 소요 시간/가격 |
| `flightType` | enum | Y | `OUTBOUND`, `RETURN` |

---

## 10. Schedule API

> Schedule API는 현재 일부 엔드포인트가 `ApiResponse<T>`가 아닌 DTO를 직접 반환합니다.

### 10.1 일정

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 일정 목록 조회 | 워크스페이스의 일정 버전 목록 조회 | `/api/v1/schedules?workspaceId={workspaceId}` | `GET` | Query: `workspaceId` | `List<ScheduleSummaryResponse>` | `200`, `401`, `404` | JWT 필요 |
| 일정 상세 조회 | 일정과 아이템 전체 조회 | `/api/v1/schedules/{scheduleId}` | `GET` | Path: `scheduleId` | `ScheduleResponse` | `200`, `401`, `404` | JWT 필요 |
| 최신 일정 조회 | 워크스페이스 최신 일정 조회 | `/api/v1/schedules/latest?workspaceId={workspaceId}` | `GET` | Query: `workspaceId` | `ScheduleResponse` | `200`, `401`, `404` | JWT 필요 |
| 일정 생성 | AI/수동 생성 결과 저장 | `/api/v1/schedules` | `POST` | Body: `ScheduleCreateRequest` | `ScheduleResponse` | `200`, `400`, `401`, `404` | JWT 필요 |
| 일정 포크 | 기존 일정을 복사해 새 버전 생성 | `/api/v1/schedules/{scheduleId}/fork` | `POST` | Query: `title` optional | `ScheduleResponse` | `200`, `401`, `404` | JWT 필요 |
| 일정 제목 수정 | 일정 제목 변경 | `/api/v1/schedules/{scheduleId}/title` | `PATCH` | Query: `title` | `ScheduleResponse` | `200`, `400`, `401`, `404` | JWT 필요 |
| 일정 삭제 | 일정 삭제 | `/api/v1/schedules/{scheduleId}` | `DELETE` | Path: `scheduleId` | 없음 | `204`, `401`, `404` | JWT 필요 |
| 일정 지도 핀 조회 | 좌표가 있는 일정 아이템을 day별 핀으로 반환 | `/api/v1/schedules/{scheduleId}/map` | `GET` | Path: `scheduleId` | `ScheduleMapResponse` | `200`, `401`, `404` | JWT 필요 |

**ScheduleCreateRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `workspaceId` | number | Y | 워크스페이스 ID |
| `title` | string | N | 일정 제목 |
| `items` | array | Y | `ScheduleItemCreateRequest` 목록 |

### 10.2 일정 아이템

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 일정 아이템 추가 | 특정 일정에 아이템 추가 | `/api/v1/schedules/{scheduleId}/items` | `POST` | Body: `ScheduleItemCreateRequest` | `ScheduleItemResponse` | `200`, `400`, `401`, `404` | JWT 필요 |
| 일정 아이템 수정 | 아이템 장소/시간/메모 등 수정 | `/api/v1/schedules/{scheduleId}/items/{itemId}` | `PATCH` | Body: `ScheduleItemUpdateRequest` | `ScheduleItemResponse` | `200`, `400`, `401`, `403`, `404` | JWT 필요 |
| 일정 아이템 위치 이동 | D&D 결과를 day/orderIndex로 반영 | `/api/v1/schedules/{scheduleId}/items/{itemId}/position` | `PATCH` | Body: `ScheduleItemMoveRequest` | 없음 | `204`, `400`, `401`, `404` | JWT 필요 |
| 일정 아이템 삭제 | 아이템 삭제 및 순서 재정렬 | `/api/v1/schedules/{scheduleId}/items/{itemId}` | `DELETE` | Path: `scheduleId`, `itemId` | 없음 | `204`, `401`, `403`, `404` | JWT 필요 |
| 딥링크 클릭 추적 | 아이템 딥링크 클릭 카운트 증가 | `/api/v1/schedules/{scheduleId}/items/{itemId}/deeplink-click` | `POST` | Path: `scheduleId`, `itemId` | 없음 | `204`, `401`, `404` | JWT 필요 |

**ScheduleItemCreateRequest / UpdateRequest 주요 필드**

| 필드 | 타입 | 생성 필수 | 설명 |
|---|---|---|---|
| `day` | integer | Y | 1 이상 |
| `orderIndex` | integer | Y | 0 이상 |
| `visitTime` | time | N | `HH:mm` |
| `category` | enum | Y | 일정 카테고리 |
| `name` | string | Y | 장소/일정명 |
| `address`, `memo`, `deepLinkUrl` | string | N | 부가 정보 |
| `latitude`, `longitude` | number | N | 좌표 |
| `placeId`, `photoReference` | string | N | Google Places 정보 |
| `estimatedCost` | number | N | 예상 비용 |

---

## 11. AI Chat API

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| ChatRoom 생성 | 워크스페이스에 AI 채팅방 생성 | `/api/v1/chat/rooms` | `POST` | Body: `ChatRoomCreateRequest` | `ChatRoomSummaryResponse` | `200`, `400`, `401` | JWT 필요 |
| ChatRoom 목록 조회 | 워크스페이스의 AI 채팅방 목록 조회 | `/api/v1/chat/workspaces/{workspaceId}/rooms` | `GET` | Path: `workspaceId` | `List<ChatRoomSummaryResponse>` | `200`, `401`, `404` | JWT 필요 |
| 메시지 조회 | 특정 ChatRoom의 메시지 전체 조회 | `/api/v1/chat/rooms/{roomId}/messages` | `GET` | Path: `roomId` | `ChatHistoryResponse` | `200`, `401`, `404` | JWT 필요 |
| 메시지 전송 | 사용자 메시지 전송 후 AI 응답 수신 | `/api/v1/chat/rooms/{roomId}` | `POST` | Body: `ChatRequest` | `ChatResponse` | `200`, `400`, `401`, `404` | JWT 필요 |
| 메시지 전송 스트리밍 | AI 응답을 SSE 청크로 수신 | `/api/v1/chat/rooms/{roomId}/stream` | `POST` | Body: `ChatRequest` | `text/event-stream` | `200`, `400`, `401`, `404` | JWT 필요 |
| AI 확정 일정 저장 | 마지막 AI JSON 응답을 Schedule로 변환 저장 | `/api/v1/chat/rooms/{roomId}/save-schedule` | `POST` | Query: `workspaceId` | `ScheduleResponse` | `200`, `401`, `404`, `422` | JWT 필요 |

**ChatRoomCreateRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `workspaceId` | number | Y | 연결할 워크스페이스 |
| `title` | string | N | 채팅방 제목 |

**ChatRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `message` | string | Y | 사용자 메시지 |

---

## 12. Messaging API

> `/api/v1/messaging/**`는 현재 SecurityConfig에서 permitAll로 열려 있습니다. 다만 컨트롤러 문서와 WebSocket 인터셉터는 인증 사용을 전제로 하므로 운영 전 인증 정책 정리가 필요합니다.

### 12.1 REST

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 채팅방 생성 | 1:1/그룹/워크스페이스 채팅방 생성 | `/api/v1/messaging/rooms` | `POST` | Body: `MessagingRoomCreateRequest` | `ApiResponse<MessagingRoom>` | `200`, `400`, `401` | 현재 permitAll. 운영: JWT 권장 |
| 메시지 히스토리 조회 | 채팅방 메시지를 페이지로 조회 | `/api/v1/messaging/rooms/{roomId}/messages` | `GET` | Query: `page=0`, `size=30` | `ApiResponse<Page<MessagingMessageResponse>>` | `200`, `401`, `404` | 현재 permitAll. 운영: 방 멤버 권장 |
| WebSocket 문서용 더미 | Swagger 노출용 WS 설명 엔드포인트 | `/api/v1/messaging/rooms/{roomId}/ws-docs` | `GET` | Path: `roomId` | 없음 | `200` | 현재 permitAll |

**MessagingRoomCreateRequest**

| 필드 | 타입 | 설명 |
|---|---|---|
| `type` | enum | 채팅방 타입 |
| `name` | string | 방 이름 |
| `workspaceId` | number | 워크스페이스 채팅방일 경우 연결 ID |
| `memberIds` | array number | 초대할 사용자 ID 목록 |

### 12.2 WebSocket/STOMP

| 항목 | 내용 |
|---|---|
| WebSocket endpoint | `/ws` |
| SockJS endpoint | `/ws` |
| Publish destination | `/pub/chat.message.{roomId}` |
| Subscribe destination | `/sub/chat/{roomId}` |
| Auth header | STOMP native header `Authorization: Bearer {accessToken}` |

**WebSocket Message Request**

| 필드 | 타입 | 설명 |
|---|---|---|
| `content` | string | 메시지 본문 |
| `type` | enum | 메시지 타입 |

**WebSocket Message Response**

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string | MongoDB 메시지 ID |
| `messagingRoomId` | number | 채팅방 ID |
| `senderId`, `senderNickname` | number/string | 발신자 |
| `content`, `type` | string/enum | 메시지 본문/타입 |
| `createdAt` | datetime | 생성 시각 |

---

## 13. Conquest Map API

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 정복 지도 전체 조회 | 내 방문 국가/도시 목록 조회 | `/api/conquest` | `GET` | 없음 | `ApiResponse<ConquestMapResponse>` | `200`, `401` | JWT 필요 |
| 여행 통계 조회 | 방문 국가/도시/일수/거리/대륙 통계 조회 | `/api/conquest/stats` | `GET` | 없음 | `ApiResponse<ConquestStatsResponse>` | `200`, `401` | JWT 필요 |
| 국가 방문 상태 변경 | 국가 코드 기준 방문 상태 변경 | `/api/conquest/countries/{countryCode}/status` | `PUT` | Body: `StatusUpdateRequest` | `ApiResponse<VisitedCountryResponse>` | `200`, `400`, `401`, `404` | JWT 필요 |
| 도시 추가/상태 설정 | 도시를 추가하거나 기존 도시 상태 변경 | `/api/conquest/cities` | `POST` | Body: `CityCreateRequest` | `ApiResponse<VisitedCityResponse>` | `201`, `400`, `401` | JWT 필요 |
| 도시 방문 상태 변경 | 도시 ID 기준 방문 상태 변경 | `/api/conquest/cities/{cityId}/status` | `PUT` | Body: `StatusUpdateRequest` | `ApiResponse<VisitedCityResponse>` | `200`, `400`, `401`, `403`, `404` | JWT 필요 |
| 방문 이력 일괄 등록 | 과거 방문 국가/도시를 한번에 등록 | `/api/conquest/bulk-import` | `POST` | Body: `BulkImportRequest` | 없음 | `204`, `400`, `401` | JWT 필요 |
| 국가별 워크스페이스 조회 | 특정 국가 목적지의 내 워크스페이스 목록 | `/api/conquest/countries/{countryCode}/workspaces` | `GET` | Path: `countryCode` | `ApiResponse<List<WorkspaceConquestResponse>>` | `200`, `401` | JWT 필요 |
| 전체 여행 경로 조회 | 내 모든 워크스페이스 항공 경로 조회 | `/api/conquest/routes` | `GET` | 없음 | `ApiResponse<List<TripRouteResponse>>` | `200`, `401` | JWT 필요 |
| 워크스페이스 여행 경로 조회 | 특정 워크스페이스 항공 경로 조회 | `/api/conquest/routes/workspaces/{workspaceId}` | `GET` | Path: `workspaceId` | `ApiResponse<List<TripRouteResponse>>` | `200`, `401`, `404` | JWT 필요 |

**Conquest Request DTO**

| DTO | 필드 |
|---|---|
| `StatusUpdateRequest` | `status: VisitStatus` |
| `CityCreateRequest` | `cityName`, `countryCode`, `latitude`, `longitude`, `status` |
| `BulkImportRequest` | `countries[{countryCode,status}]`, `cities[{cityName,countryCode,latitude,longitude,status}]` |

**TripRouteResponse 주요 필드**

| 필드 | 설명 |
|---|---|
| `flightId`, `workspaceId`, `workspaceTitle` | 항공편/워크스페이스 식별 정보 |
| `departureAirport`, `arrivalAirport` | 공항 코드 |
| `departureCity`, `arrivalCity` | 도시명 |
| `departureCountryCode`, `arrivalCountryCode` | 국가 코드 |
| `departureLat`, `departureLng`, `arrivalLat`, `arrivalLng` | 경로 좌표 |
| `departureTime`, `arrivalTime` | 출발/도착 시간 |
| `airline`, `flightNumber`, `distanceKm` | 항공편 표시 정보 |
| `routeType` | `INTERNATIONAL`, `DOMESTIC` |

---

## 14. Album API

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 앨범 조회 | 워크스페이스 앨범과 사진 목록 조회 | `/api/workspaces/{workspaceId}/album` | `GET` | Path: `workspaceId` | `ApiResponse<AlbumResponse>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 사진 업로드 | 사진 1장 이상 S3 업로드 후 DB 저장 | `/api/workspaces/{workspaceId}/album/photos` | `POST` | `multipart/form-data`, `files` | `ApiResponse<List<PhotoResponse>>` | `200`, `400`, `401`, `403`, `404`, `500` | JWT + 업로드 권한 |
| 사진 삭제 | S3와 DB에서 사진 삭제 | `/api/workspaces/{workspaceId}/album/photos/{photoId}` | `DELETE` | Path: `photoId` | 없음 | `204`, `401`, `403`, `404`, `500` | JWT + 삭제 권한 |
| 다운로드 URL 발급 | 사진 다운로드용 Presigned URL 발급 | `/api/workspaces/{workspaceId}/album/photos/{photoId}/download` | `GET` | Path: `photoId` | `ApiResponse<DownloadUrlResponse>` | `200`, `401`, `403`, `404`, `500` | JWT + 워크스페이스 멤버 |

**AlbumResponse / PhotoResponse**

| DTO | 주요 필드 |
|---|---|
| `AlbumResponse` | `albumId`, `workspaceId`, `photos` |
| `PhotoResponse` | `id`, `url`, `uploadedById`, `uploadedByNickname`, `takenAt`, `latitude`, `longitude`, `matchedDay`, `createdAt` |
| `DownloadUrlResponse` | `downloadUrl` |

**사진 업로드 제약**

| 항목 | 값 |
|---|---|
| 최대 파일 수 | 20장 |
| 최대 파일 크기 | 10MB |
| 허용 확장/형식 | `jpeg`, `png`, `webp`, `heic` |

---

## 15. TravelLog API

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 여행기 목록 조회 | 워크스페이스 여행기 목록 조회. content 제외 | `/api/workspaces/{workspaceId}/travellogs` | `GET` | Path: `workspaceId` | `ApiResponse<List<TravellogSummaryResponse>>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 여행기 단건 조회 | 여행기 상세 및 첨부 사진 조회 | `/api/workspaces/{workspaceId}/travellogs/{logId}` | `GET` | Path: `logId` | `ApiResponse<TravellogResponse>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 여행기 생성 | 새 여행기 생성 | `/api/workspaces/{workspaceId}/travellogs` | `POST` | Body: `TravellogCreateRequest` | `ApiResponse<TravellogResponse>` | `201`, `400`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 여행기 수정 | 여행기 부분 수정. null 필드 미변경 | `/api/workspaces/{workspaceId}/travellogs/{logId}` | `PATCH` | Body: `TravellogUpdateRequest` | `ApiResponse<TravellogResponse>` | `200`, `400`, `401`, `403`, `404` | JWT + 작성/수정 권한 |
| 여행기 삭제 | 여행기 삭제 | `/api/workspaces/{workspaceId}/travellogs/{logId}` | `DELETE` | Path: `logId` | 없음 | `204`, `401`, `403`, `404` | JWT + 삭제 권한 |
| 사진 업로드 후 첨부 | 새 사진 업로드 후 여행기에 연결 | `/api/workspaces/{workspaceId}/travellogs/{logId}/photos/upload` | `POST` | `multipart/form-data`, `files` | `ApiResponse<TravellogResponse>` | `200`, `400`, `401`, `403`, `404`, `500` | JWT + 워크스페이스 멤버 |
| 앨범 사진 첨부 | 기존 앨범 사진을 여행기에 연결 | `/api/workspaces/{workspaceId}/travellogs/{logId}/photos` | `POST` | Body: `TravellogPhotoLinkRequest` | `ApiResponse<TravellogResponse>` | `200`, `400`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 첨부 사진 해제 | 여행기와 사진 연결 해제. 앨범 사진은 유지 | `/api/workspaces/{workspaceId}/travellogs/{logId}/photos` | `DELETE` | Body: `TravellogPhotoLinkRequest` | `ApiResponse<TravellogResponse>` | `200`, `400`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |

**TravelLog Request DTO**

| DTO | 필드 |
|---|---|
| `TravellogCreateRequest` | `day`, `travelDate`, `title`, `content`, `weather` |
| `TravellogUpdateRequest` | `day`, `travelDate`, `title`, `content`, `weather` |
| `TravellogPhotoLinkRequest` | `photoIds: Long[]` |

**TravelLog Response DTO**

| DTO | 주요 필드 |
|---|---|
| `TravellogSummaryResponse` | `id`, `day`, `travelDate`, `title`, `weather`, `photoCount`, `createdAt` |
| `TravellogResponse` | `id`, `day`, `travelDate`, `title`, `content`, `weather`, `workspaceId`, `authorId`, `authorNickname`, `photos`, `createdAt`, `updatedAt` |

---

## 16. Workspace SNS API (Planned)

> 현재 코드에는 Workspace SNS 컨트롤러가 없습니다. 아래 명세는 향후 구현을 위한 제안안이며, 기존 인증/워크스페이스 권한 정책과 응답 포맷을 맞추는 기준으로 작성했습니다.

### 16.1 피드

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 워크스페이스 피드 목록 조회 | 워크스페이스 내 SNS 게시글 최신순 조회 | `/api/workspaces/{workspaceId}/sns/posts` | `GET` | Query: `page=0`, `size=20`, `sort=latest` | `ApiResponse<Page<WorkspaceSnsPostResponse>>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 피드 게시글 상세 조회 | 게시글 본문, 첨부 사진, 댓글 요약 조회 | `/api/workspaces/{workspaceId}/sns/posts/{postId}` | `GET` | Path: `postId` | `ApiResponse<WorkspaceSnsPostResponse>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 피드 게시글 작성 | 텍스트/사진/여행기 링크 기반 게시글 작성 | `/api/workspaces/{workspaceId}/sns/posts` | `POST` | Body: `WorkspaceSnsPostCreateRequest` | `ApiResponse<WorkspaceSnsPostResponse>` | `201`, `400`, `401`, `403`, `404` | JWT + `OWNER`/`EDITOR` |
| 피드 게시글 수정 | 작성자가 게시글 본문/첨부 수정 | `/api/workspaces/{workspaceId}/sns/posts/{postId}` | `PATCH` | Body: `WorkspaceSnsPostUpdateRequest` | `ApiResponse<WorkspaceSnsPostResponse>` | `200`, `400`, `401`, `403`, `404` | JWT + 작성자 |
| 피드 게시글 삭제 | 작성자 또는 OWNER가 게시글 삭제 | `/api/workspaces/{workspaceId}/sns/posts/{postId}` | `DELETE` | Path: `postId` | 없음 | `204`, `401`, `403`, `404` | JWT + 작성자 또는 `OWNER` |

**WorkspaceSnsPostCreateRequest**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `content` | string | Y | 게시글 본문 |
| `photoIds` | array number | N | 워크스페이스 앨범 사진 ID |
| `travellogId` | number | N | 연결할 여행기 ID |
| `visibility` | enum | N | `WORKSPACE_ONLY` 기본값 |

### 16.2 댓글/좋아요

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 댓글 목록 조회 | 게시글 댓글 목록 조회 | `/api/workspaces/{workspaceId}/sns/posts/{postId}/comments` | `GET` | Query: `page=0`, `size=50` | `ApiResponse<Page<WorkspaceSnsCommentResponse>>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 댓글 작성 | 게시글에 댓글 작성 | `/api/workspaces/{workspaceId}/sns/posts/{postId}/comments` | `POST` | Body: `WorkspaceSnsCommentCreateRequest` | `ApiResponse<WorkspaceSnsCommentResponse>` | `201`, `400`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 댓글 삭제 | 작성자 또는 OWNER가 댓글 삭제 | `/api/workspaces/{workspaceId}/sns/posts/{postId}/comments/{commentId}` | `DELETE` | Path: `commentId` | 없음 | `204`, `401`, `403`, `404` | JWT + 작성자 또는 `OWNER` |
| 좋아요 토글 | 게시글 좋아요 추가/취소 | `/api/workspaces/{workspaceId}/sns/posts/{postId}/likes` | `POST` | 없음 | `ApiResponse<WorkspaceSnsLikeResponse>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |

**WorkspaceSnsPostResponse 제안 필드**

| 필드 | 타입 | 설명 |
|---|---|---|
| `id`, `workspaceId` | number | 게시글/워크스페이스 ID |
| `authorId`, `authorNickname`, `authorProfileImageUrl` | number/string | 작성자 정보 |
| `content` | string | 본문 |
| `photos` | array `PhotoResponse` | 첨부 사진 |
| `travellog` | object | 연결된 여행기 요약 |
| `likeCount`, `commentCount`, `likedByMe` | number/boolean | 반응 정보 |
| `createdAt`, `updatedAt` | datetime | 생성/수정 시각 |

### 16.3 알림/공유

| API명 | 역할/요약 | 엔드포인트 | Method | Request | Response | 상태코드 | 인증/인가 |
|---|---|---|---|---|---|---|---|
| 피드 공유 링크 생성 | 게시글 공유용 토큰 URL 생성 | `/api/workspaces/{workspaceId}/sns/posts/{postId}/share-link` | `POST` | 없음 | `ApiResponse<WorkspaceSnsShareLinkResponse>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| 내 SNS 알림 목록 조회 | 댓글/좋아요/멘션 알림 조회 | `/api/workspaces/{workspaceId}/sns/notifications` | `GET` | Query: `page=0`, `size=20`, `unreadOnly=false` | `ApiResponse<Page<WorkspaceSnsNotificationResponse>>` | `200`, `401`, `403`, `404` | JWT + 워크스페이스 멤버 |
| SNS 알림 읽음 처리 | 알림을 읽음 상태로 변경 | `/api/workspaces/{workspaceId}/sns/notifications/{notificationId}/read` | `PATCH` | Path: `notificationId` | `ApiResponse<WorkspaceSnsNotificationResponse>` | `200`, `401`, `403`, `404` | JWT + 알림 수신자 |

---

## 17. 예외 코드 요약

| Domain | Code | HTTP | Message |
|---|---|---:|---|
| Auth | `EMPTY_AUTHENTICATION` | 401 | 인증 정보가 존재하지 않습니다. |
| Auth | `TOKEN_EXPIRED` | 401 | 토큰이 만료되었습니다. |
| Auth | `INVALID_TOKEN` | 401 | 유효하지 않은 토큰입니다. |
| Auth | `INVALID_REFRESH_TOKEN` | 401 | 유효하지 않은 리프레시 토큰입니다. |
| Auth | `ACCESS_DENIED` | 403 | 접근 권한이 없습니다. |
| User | `USER_001` | 404 | 사용자를 찾을 수 없습니다. |
| User | `INVALID_BUDGET_RANGE` | 400 | 최소 예산은 최대 예산보다 클 수 없습니다. |
| Workspace | `WORKSPACE_001` | 404 | 워크스페이스를 찾을 수 없습니다. |
| Workspace | `WORKSPACE_002` | 403 | 워크스페이스에 대한 권한이 없습니다. |
| Workspace | `WORKSPACE_003` | 409 | 이미 워크스페이스 멤버입니다. |
| Workspace | `WORKSPACE_004` | 404 | 멤버를 찾을 수 없습니다. |
| Workspace | `WORKSPACE_005` | 400 | 유효하지 않은 초대 코드입니다. |
| Workspace | `WORKSPACE_006` | 400 | 워크스페이스 소유자는 탈퇴할 수 없습니다. |
| Workspace | `WORKSPACE_007` | 400 | 소유자 역할은 직접 변경할 수 없습니다. |
| Conquest | `CONQUEST_001` | 404 | 방문 국가 정보를 찾을 수 없습니다. |
| Conquest | `CONQUEST_002` | 404 | 방문 도시 정보를 찾을 수 없습니다. |
| Conquest | `CONQUEST_003` | 403 | 해당 정복 정보에 접근 권한이 없습니다. |
| Common | `COMMON_002` | 400 | 잘못된 요청입니다. |
| Common | `COMMON404` | 404 | 해당 리소스를 찾을 수 없습니다. |
| Common | `COMMON409` | 409 | 데이터 충돌이 발생했습니다. |
| Common | `COMMON_001` | 500 | 서버 오류가 발생했습니다. |

---

## 18. 프론트엔드 연동 체크리스트

| 체크 | 내용 |
|---|---|
| Token Refresh | `401` 발생 시 `/api/auth/refresh` 호출 후 원 요청 재시도 |
| Time Format | 날짜는 `yyyy-MM-dd`, 시간은 `HH:mm`, 항공 저장 시간은 timezone 포함 `ZonedDateTime` 사용 |
| Multipart | 앨범/여행기 사진 업로드는 `multipart/form-data` + `files` 키 사용 |
| SSE | AI 스트리밍은 `text/event-stream` 응답이며, 중간 chunk를 누적 표시 |
| WebSocket | STOMP native header에 `Authorization` 전달 |
| Workspace Role | 수정/삭제/초대/역할 변경은 `OWNER` 조건을 UI에서 선제 제어 |
| Error Message | 도메인 예외는 `code` 없이 `message`만 내려올 수 있어 message 우선 표시 |
