package com.sofly.core.domain.user.entity;

import com.sofly.core.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;          // GOOGLE, KAKAO

    @Column(nullable = false)
    private String providerId;          // OAuth2 고유 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    // 여행 성향 프로필
    @Enumerated(EnumType.STRING)
    private AgeGroup ageGroup;          // TEENS, TWENTIES, THIRTIES, FORTIES_PLUS

    private String city;                // 거주 도시

    @Enumerated(EnumType.STRING)
    private CompanionType preferCompanionType; // SOLO, COUPLE, FRIENDS, FAMILY

    private Integer budgetMin;          // 선호 예산 최솟값 (원)
    private Integer budgetMax;          // 선호 예산 최댓값 (원)

    // 선호 테마 (복수) — 별도 테이블
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_prefer_themes",
            joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "theme")
    @Builder.Default
    private List<TravelTheme> preferThemes = new ArrayList<>();

    // 선호 도시 태그
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_prefer_cities",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "city_name")
    @Builder.Default
    private List<String> preferCities = new ArrayList<>();

    // ── 비즈니스 메서드 ──────────────────────────────────────

    public void updateProfile(String nickname, String city, AgeGroup ageGroup,
                              CompanionType companionType, Integer budgetMin, Integer budgetMax) {
        this.nickname = nickname;
        this.city = city;
        this.ageGroup = ageGroup;
        this.preferCompanionType = companionType;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
    }

    public void updateProfileImage(String url) {
        this.profileImageUrl = url;
    }

    public void updatePreferThemes(List<TravelTheme> themes) {
        // 추가할 것만 add
        themes.stream()
            .filter(theme -> !this.preferThemes.contains(theme))
            .forEach(this.preferThemes::add);
        
        // 삭제할 것만 remove
        this.preferThemes.removeIf(theme -> !themes.contains(theme));
    }

    public void updatePreferCities(List<String> cities) {
        cities.stream()
            .filter(city -> !this.preferCities.contains(city))
            .forEach(this.preferCities::add);
        
        this.preferCities.removeIf(city -> !cities.contains(city));
    }

    // ── Enum ────────────────────────────────────────────────

    public enum Provider { GOOGLE, KAKAO }

    public enum Role { USER, ADMIN }

    public enum AgeGroup { TEENS, TWENTIES, THIRTIES, FORTIES_PLUS }

    public enum CompanionType { SOLO, COUPLE, FRIENDS, FAMILY }

    public enum TravelTheme { RELAXATION, SHOPPING, CULTURE, ACTIVITY, FOOD }
}
