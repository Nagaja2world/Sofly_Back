package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.sns.entity.SnsPost;

import java.time.LocalDateTime;
import java.util.List;

public record SnsPostResponse(
        Long id,
        Long workspaceId,
        AuthorInfo author,
        String content,
        SnsPost.Visibility visibility,
        List<SnsPostImageResponse> images,
        LocalDateTime createdAt
) {
    public static SnsPostResponse from(SnsPost post) {
        List<SnsPostImageResponse> imageResponses = post.getImages().stream()
                .map(SnsPostImageResponse::from)
                .toList();

        return new SnsPostResponse(
                post.getId(),
                post.getWorkspace().getId(),
                AuthorInfo.from(post.getAuthor()),
                post.getContent(),
                post.getVisibility(),
                imageResponses,
                post.getCreatedAt()
        );
    }
}
