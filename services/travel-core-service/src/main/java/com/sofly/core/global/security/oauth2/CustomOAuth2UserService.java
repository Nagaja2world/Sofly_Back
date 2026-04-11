package com.sofly.core.global.security.oauth2;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.global.security.oauth2.userinfo.GoogleUserInfo;
import com.sofly.core.global.security.oauth2.userinfo.KakaoUserInfo;
import com.sofly.core.global.security.oauth2.userinfo.NaverUserInfo;
import com.sofly.core.global.security.oauth2.userinfo.OAuth2UserInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 제공자 구분 (google / kakao / naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, oAuth2User);

        // 신규 가입 or 기존 회원 조회
        User user = saveOrUpdate(registrationId, userInfo);

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    // ── 제공자별 UserInfo 파싱 ───────────────────────────
    private OAuth2UserInfo resolveUserInfo(String registrationId, OAuth2User oAuth2User) {
        return switch (registrationId) {
            case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
            case "kakao"  -> new KakaoUserInfo(oAuth2User.getAttributes());
            case "naver"  -> new NaverUserInfo(oAuth2User.getAttributes());
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        };
    }

    // ── 신규 가입 or 기존 회원 반환 ──────────────────────
    private User saveOrUpdate(String registrationId, OAuth2UserInfo userInfo) {
        User.Provider provider = User.Provider.valueOf(registrationId.toUpperCase());
        
        return userRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                .orElseGet(() -> {
                    if (userRepository.existsByEmail(userInfo.getEmail())) {
                        throw new OAuth2AuthenticationException(
                            new OAuth2Error("duplicate_email", "이미 다른 소셜 계정으로 가입된 이메일입니다.", null)
                        );
                    }
                    return userRepository.save(User.builder()
                            .email(userInfo.getEmail())
                            .nickname(userInfo.getNickname())
                            .profileImageUrl(userInfo.getProfileImageUrl())
                            .provider(provider)
                            .providerId(userInfo.getProviderId())
                            .role(User.Role.USER)
                            .build());
                });
    }
}
