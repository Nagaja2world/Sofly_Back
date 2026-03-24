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
        return response != null ? (String) response.get("id") : null;
    }

    @Override
    public String getEmail() {
        return response != null ? (String) response.get("email") : null;
    }

    @Override
    public String getNickname() {
        return response != null ? (String) response.get("name") : null;
    }

    @Override
    public String getProfileImageUrl() {
        return response != null ? (String) response.get("profile_image") : null;
    }
}
