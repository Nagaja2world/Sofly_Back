package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.sns.entity.SnsPost;

public record SnsPostUpdateRequest(
        String content,
        SnsPost.Visibility visibility
) {}
