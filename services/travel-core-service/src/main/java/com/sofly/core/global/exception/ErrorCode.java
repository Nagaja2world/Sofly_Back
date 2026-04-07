package com.sofly.core.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Auth ─────────────────────────────────────────────
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh Token을 찾을 수 없습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access Token입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // ── User ─────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // ── Workspace ────────────────────────────────────────
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다."),
    WORKSPACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "워크스페이스에 접근 권한이 없습니다."),
    WORKSPACE_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "워크스페이스 멤버를 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 코드입니다."),
    ALREADY_WORKSPACE_MEMBER(HttpStatus.CONFLICT, "이미 워크스페이스 멤버입니다."),

    // ── Schedule ─────────────────────────────────────────
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    SCHEDULE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "일정 항목을 찾을 수 없습니다."),

    // ── Album ────────────────────────────────────────────
    ALBUM_NOT_FOUND(HttpStatus.NOT_FOUND, "앨범을 찾을 수 없습니다."),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "사진을 찾을 수 없습니다."),
    UPLOAD_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "사진 업로드 권한이 없습니다."),
    DELETE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "사진 삭제 권한이 없습니다."),
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 업로드 처리 중 오류가 발생했습니다."),

    // ── TravelLog ────────────────────────────────────────
    TRAVEL_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "여행기를 찾을 수 없습니다."),
    TRAVEL_LOG_ACCESS_DENIED(HttpStatus.FORBIDDEN, "여행기에 접근 권한이 없습니다."),

    // ── Common ───────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
