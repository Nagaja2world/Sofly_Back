package com.sofly.core.global.security.oauth2.userinfo;

public interface OAuth2UserInfo {

    String getProviderId();

    String getEmail();

    String getNickname();

    String getProfileImageUrl();
}
