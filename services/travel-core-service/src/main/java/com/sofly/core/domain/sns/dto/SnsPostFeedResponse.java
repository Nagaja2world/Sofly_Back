package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.sns.entity.SnsPost;

import java.time.LocalDateTime;

public record SnsPostFeedResponse(
        Long id,
        Long workspaceId,
        AuthorInfo author,
        String content,
        SnsPost.Visibility visibility,
        String firstImageUrl,
        int imageCount,
        LocalDateTime createdAt
) {
    public static SnsPostFeedResponse from(SnsPost post) {
        String firstImage = post.getImages().isEmpty() ? null : post.getImages().get(0).getUrl();
        return new SnsPostFeedResponse(
                post.getId(),
                post.getWorkspace().getId(),
                AuthorInfo.from(post.getAuthor()),
                post.getContent(),
                post.getVisibility(),
                firstImage,
                post.getImages().size(),
                post.getCreatedAt()
        );
    }
}
