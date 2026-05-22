# SNS Phase 2 설계 문서

**작성일:** 2026-05-22  
**브랜치:** feat/77-sns  
**담당:** sehyeon

---

## 1. 목표

인스타그램 스타일 SNS 기능 완성. Phase 1(데이터 레이어)은 완료된 상태에서 Phase 2~4를 구현하고, 코드 리뷰에서 발견된 Critical 버그 4개를 먼저 수정한다.

---

## 2. 현재 상태

### 완료
- `WorkspaceVisibility` enum (PUBLIC/PRIVATE)
- `Workspace.visibility` 필드 + `changeVisibility()` 메서드
- `UserFollow`, `WorkspaceLike`, `WorkspaceComment` 엔티티
- `SnsErrorCode`, `SnsException`
- `UserFollowRepository`, `WorkspaceLikeRepository`, `WorkspaceCommentRepository`
- `WorkspaceRepository` SNS 쿼리 4개
- `GlobalExceptionHandler.SnsException` 핸들러
- `FollowService` + `FollowController`
- `WorkspaceService.changeVisibility()`
- `LikeService` (버그 있음)
- `WorkspaceLikeController` (빈 껍데기)

### Critical 버그
| # | 파일 | 문제 | 수정 방법 |
|---|------|------|-----------|
| 1 | `LikeService.like()` | `@Transactional` 누락, 중복 좋아요 체크 없음 | `@Transactional` 추가 + `existsByWorkspaceIdAndUserId` 체크 |
| 2 | `LikeService.unlike()` | `@Transactional` 누락 | `@Transactional` 추가 |
| 3 | `FollowService`, `LikeService` | `EntityNotFoundException` → 500 반환 | `UserException(USER_NOT_FOUND)`, `WorkspaceException(WORKSPACE_NOT_FOUND)` 교체 |
| 4 | `WorkspaceService.changeVisibility()` | `validateMember` → 일반 멤버도 공개범위 변경 가능 | `validateOwner` 교체 |

---

## 3. 아키텍처

기존 레이어드 아키텍처(Controller → Service → Repository) 그대로 따른다.

### 패키지 구조
```
domain/sns/
├── controller/  FollowController (기존), WorkspaceLikeController (보완),
│                WorkspaceCommentController (신규), FeedController (신규),
│                SearchController (신규), PublicProfileController (신규)
├── code/        SnsErrorCode (기존)
├── dto/         PublicWorkspaceResponse (신규), AuthorInfo (신규),
│                ScheduleSummary (신규), FollowStatsResponse (신규),
│                CommentResponse (신규), CommentCreateRequest (신규),
│                CommentUpdateRequest (신규), PublicUserProfileResponse (신규)
├── entity/      UserFollow, WorkspaceLike, WorkspaceComment (기존)
├── exception/   SnsException (기존)
├── repository/  기존 (WorkspaceCommentRepository에 배치 카운트 쿼리 추가)
└── service/     FollowService (기존), LikeService (버그 수정), 
                 CommentService (신규), FeedService (신규),
                 SearchService (신규), PublicProfileService (신규)
```

---

## 4. 엔티티 / 데이터 변경

### WorkspaceVisibility
```java
public enum WorkspaceVisibility {
    PUBLIC,
    FOLLOWERS_ONLY,  // 추가
    PRIVATE
}
```
> `FOLLOWERS_ONLY` 접근 제어: PublicProfileService/FeedService에서 viewer가 owner를 팔로우하는지 확인. 미인증 사용자는 FOLLOWERS_ONLY 워크스페이스 접근 불가.

### WorkspaceRepository 추가
```java
// findPublicById → findByIdAndVisibility(PUBLIC) 로 대체 가능 (이미 존재)
```

### WorkspaceCommentRepository 추가
```java
@Query("SELECT wc.workspace.id, COUNT(wc) FROM WorkspaceComment wc " +
       "WHERE wc.workspace.id IN :workspaceIds GROUP BY wc.workspace.id")
List<Object[]> countByWorkspaceIds(@Param("workspaceIds") List<Long> workspaceIds);

long countByWorkspaceId(Long workspaceId);
```

### ScheduleRepository 추가
```java
// 워크스페이스의 최신 버전 일정 1개
@Query("SELECT s FROM Schedule s JOIN FETCH s.items WHERE s.workspace.id = :workspaceId " +
       "ORDER BY s.version DESC")
Optional<Schedule> findLatestByWorkspaceId(@Param("workspaceId") Long workspaceId);
```

---

## 5. DTO 설계

### PublicWorkspaceResponse
공개 피드/검색/프로필에 공통으로 사용되는 읽기 전용 DTO.

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 워크스페이스 ID |
| `title` | String | 여행 제목 |
| `destination` | String | 목적지 |
| `countryCode` | String | ISO 코드 |
| `startDate` | LocalDate | |
| `endDate` | LocalDate | |
| `headcount` | Integer | 인원 수 (이름 비공개) |
| `coverImageUrl` | String | 커버 이미지 |
| `visibility` | WorkspaceVisibility | |
| `author` | AuthorInfo | 작성자 (닉네임, 프로필이미지) |
| `likeCount` | long | |
| `commentCount` | long | |
| `isLiked` | Boolean | 현재 사용자 좋아요 여부 (미인증 시 null) |
| `latestSchedule` | ScheduleSummary | 최신 버전 일정 (없으면 null) |
| `createdAt` | LocalDateTime | |

**주의:** `inviteCode` 미포함.

### AuthorInfo
```java
record AuthorInfo(Long userId, String nickname, String profileImageUrl)
```

### ScheduleSummary
최신 Schedule 버전의 아이템을 day별로 그룹핑.
```java
record ScheduleSummary(Integer version, Map<Integer, List<ScheduleItemSummary>> itemsByDay)
record ScheduleItemSummary(Integer day, Integer orderIndex, String placeName, String memo)
```

### FollowStatsResponse
```java
record FollowStatsResponse(long followerCount, long followingCount, boolean isFollowing)
```
> `isFollowing`: 현재 사용자가 target을 팔로우 중인지. 미인증 시 false.

### CommentResponse
```java
record CommentResponse(Long id, Long authorId, String authorNickname, String authorProfileImageUrl, 
                       String content, LocalDateTime createdAt, LocalDateTime updatedAt)
```

### CommentCreateRequest / CommentUpdateRequest
```java
record CommentCreateRequest(@NotBlank @Size(max = 500) String content)
record CommentUpdateRequest(@NotBlank @Size(max = 500) String content)
```

### PublicUserProfileResponse
```java
record PublicUserProfileResponse(Long userId, String nickname, String profileImageUrl,
                                 long followerCount, long followingCount, boolean isFollowing,
                                 Page<PublicWorkspaceResponse> publicWorkspaces)
```

---

## 6. API 엔드포인트

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/sns/users/{targetUserId}/follow` | 필수 | 팔로우 |
| DELETE | `/api/sns/users/{targetUserId}/follow` | 필수 | 언팔로우 |
| GET | `/api/sns/users/{targetUserId}/follow-stats` | 선택 | 팔로워/팔로잉 수 + 팔로우 여부 |
| GET | `/api/sns/users/{targetUserId}/profile` | 선택 | 공개 프로필 + 공개 워크스페이스 목록 |
| POST | `/api/sns/workspaces/{workspaceId}/likes` | 필수 | 좋아요 |
| DELETE | `/api/sns/workspaces/{workspaceId}/likes` | 필수 | 좋아요 취소 |
| GET | `/api/sns/workspaces/{workspaceId}/comments` | 선택 | 댓글 목록 (페이징) |
| POST | `/api/sns/workspaces/{workspaceId}/comments` | 필수 | 댓글 작성 |
| PATCH | `/api/sns/workspaces/{workspaceId}/comments/{commentId}` | 필수 | 댓글 수정 (본인만) |
| DELETE | `/api/sns/workspaces/{workspaceId}/comments/{commentId}` | 필수 | 댓글 삭제 (본인만) |
| GET | `/api/sns/feed` | 필수 | 알고리즘 피드 (페이징) |
| GET | `/api/sns/workspaces/search` | 선택 | 검색 (countryCode, keyword) |

### SecurityConfig permitAll 추가
```java
.requestMatchers(HttpMethod.GET, "/api/sns/workspaces/search").permitAll()
.requestMatchers(HttpMethod.GET, "/api/sns/workspaces/*/comments").permitAll()
.requestMatchers(HttpMethod.GET, "/api/sns/users/*/profile").permitAll()
.requestMatchers(HttpMethod.GET, "/api/sns/users/*/follow-stats").permitAll()
```

---

## 7. 피드 알고리즘

```
후보 수집 (최대 200개):
  1. 내가 팔로우한 유저들의 PUBLIC 워크스페이스 (최신 100개)
  2. 최근 7일 이내 생성된 전체 PUBLIC 워크스페이스 (최신 100개)
  → Set으로 중복 제거

점수 계산 (인메모리):
  score = (likeCount × 3) + (commentCount × 2) + (7일 이내 생성 시 +10)

N+1 방지:
  - countByWorkspaceIds() 배치 쿼리로 좋아요/댓글 수 Map 생성
  - Map<workspaceId, count>로 매핑

페이지 슬라이스:
  - 점수 내림차순 정렬 후 Pageable 적용
```

---

## 8. 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| SNS_001 CANNOT_FOLLOW_SELF | 400 | |
| SNS_002 ALREADY_FOLLOWING | 409 | |
| SNS_003 FOLLOW_NOT_FOUND | 404 | |
| SNS_004 ALREADY_LIKED | 409 | |
| SNS_005 LIKE_NOT_FOUND | 404 | |
| SNS_006 WORKSPACE_NOT_PUBLIC | 403 | |
| SNS_007 COMMENT_NOT_FOUND | 404 | |
| SNS_008 COMMENT_FORBIDDEN | 403 | |

---

## 9. 구현 순서 (태스크)

### Critical 버그 수정 (즉시)
1. `LikeService` - `@Transactional` + 중복 체크 + 도메인 예외
2. `FollowService.getUserOrThrow()` - `UserException(USER_NOT_FOUND)`
3. `WorkspaceService.changeVisibility()` - `validateOwner`

### Phase 2 완성
4. `WorkspaceVisibility.FOLLOWERS_ONLY` 추가
5. `WorkspaceLikeController` 엔드포인트 구현
6. `WorkspaceCommentRepository` 카운트 쿼리 추가
7. `CommentService` + `WorkspaceCommentController`
8. `SecurityUtils.tryGetCurrentUserId()` + `SecurityConfig` permitAll

### Phase 3
9. `ScheduleRepository.findLatestByWorkspaceId()` 추가
10. DTO 생성: `AuthorInfo`, `ScheduleSummary`, `PublicWorkspaceResponse`, `FollowStatsResponse`, `CommentResponse`, `CommentCreateRequest/UpdateRequest`, `PublicUserProfileResponse`
11. `SearchService` + `SearchController`
12. `PublicProfileService` + `PublicProfileController`
13. `FeedService` + `FeedController`

### Phase 4
14. `WorkspaceResponse.visibility` 필드 추가
15. `LikeService.UserWithWorkspace` record → `private`

---

## 10. 설계 결정 사항

- **FOLLOWERS_ONLY 접근 제어:** 미인증 사용자는 항상 거부. 인증된 사용자는 viewer가 owner를 팔로우하는지 확인.
- **피드 인증:** 필수 (팔로우 기반이므로). 미인증 시 401.
- **댓글 수정/삭제 권한:** 댓글 작성자 본인만 가능 (`COMMENT_FORBIDDEN`).
- **PublicWorkspaceResponse.isLiked:** 미인증 시 `null`로 반환 (Boolean 타입 사용).
- **최신 일정:** version 기준 가장 높은 Schedule 1개만 노출.
- **배치 카운트:** `countByWorkspaceIds()` 쿼리를 LikeRepository와 CommentRepository 양쪽에 구현하여 N+1 방지.
