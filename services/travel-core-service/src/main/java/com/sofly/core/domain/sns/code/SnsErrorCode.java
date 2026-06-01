package com.sofly.core.domain.sns.code;

import com.sofly.core.global.response.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SnsErrorCode implements BaseErrorCode {

    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "SNS_001", "자기 자신을 팔로우할 수 없습니다."),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "SNS_002", "이미 팔로우 중입니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "SNS_003", "팔로우 관계를 찾을 수 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "SNS_004", "이미 좋아요를 눌렀습니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "SNS_005", "좋아요를 찾을 수 없습니다."),
    WORKSPACE_NOT_PUBLIC(HttpStatus.FORBIDDEN, "SNS_006", "공개된 워크스페이스가 아닙니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SNS_007", "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "SNS_008", "댓글 수정/삭제 권한이 없습니다."),
    SNS_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "SNS_009", "SNS 게시물을 찾을 수 없습니다."),
    SNS_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "SNS_010", "SNS 게시물에 접근 권한이 없습니다."),
    SNS_POST_ALREADY_EXISTS(HttpStatus.CONFLICT, "SNS_011", "이미 SNS 게시물이 존재합니다. 워크스페이스당 하나만 작성할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
