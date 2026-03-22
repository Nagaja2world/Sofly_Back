package com.sofly.core.global.security.oauth2.userinfo;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo {

    // user-name-attribute: response → attributes 안에 "response" 키로 실제 데이터가 있음
    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverUserInfo(Map<String, Object> attributes) {
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderId() {
        return (String) response.get("id");
    }

    @Override
    public String getEmail() {
        return (String) response.get("email");
    }

    @Override
    public String getNickname() {
        return (String) response.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) response.get("profile_image");
    }
}
