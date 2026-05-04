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
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, String.format("파일 크기는 %dMB를 초과할 수 없습니다.", 10)),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다. (jpeg, png, webp, heic만 가능)"),
    TOO_MANY_FILES(HttpStatus.BAD_REQUEST, String.format("한 번에 최대 %d장까지 업로드할 수 있습니다.", 20)),
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 업로드 처리 중 오류가 발생했습니다."),
    S3_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 다운로드 URL 생성 중 오류가 발생했습니다."),
    S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 삭제 중 오류가 발생했습니다."),

    // ── TravelLog ────────────────────────────────────────
    TRAVEL_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "여행기를 찾을 수 없습니다."),
    TRAVEL_LOG_ACCESS_DENIED(HttpStatus.FORBIDDEN, "여행기에 접근 권한이 없습니다."),

    // ── Chat ─────────────────────────────────────────────
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "저장된 AI 응답 메시지가 없습니다."),
    INVALID_AI_RESPONSE(HttpStatus.UNPROCESSABLE_ENTITY, "AI 응답을 일정으로 변환할 수 없습니다. 먼저 일정을 확정해주세요."),

    // ── Message ──────────────────────────────────────────

    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    MESSAGING_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "채팅방에 접근 권한이 없습니다."),

    // ── Supply ───────────────────────────────────────────
    SUPPLY_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "공급 서비스 호출 중 오류가 발생했습니다."),

    // ── Common ───────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
