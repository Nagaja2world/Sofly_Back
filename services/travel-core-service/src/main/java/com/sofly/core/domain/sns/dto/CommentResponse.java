package com.sofly.core.domain.sns.dto;

import com.sofly.core.domain.sns.entity.WorkspaceComment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long authorId,
        String authorNickname,
        String authorProfileImageUrl,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentResponse from(WorkspaceComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getNickname(),
                comment.getAuthor().getProfileImageUrl(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
