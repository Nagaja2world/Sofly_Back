package com.sofly.core.global.exception;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;

import com.sofly.core.domain.conquest.exception.ConquestException;
import com.sofly.core.domain.sns.exception.SnsException;
import com.sofly.core.domain.user.exception.UserException;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.global.response.ApiResponse;
import com.sofly.core.global.response.code.BaseErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── SoflyException (비즈니스 예외) ───────────────────
    @ExceptionHandler(SoflyException.class)
    public ResponseEntity<ApiResponse<Void>> handleSoflyException(SoflyException e) {
        log.warn("SoflyException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(errorCode.getMessage()));
    }

    @ExceptionHandler(WorkspaceException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorkspaceException(WorkspaceException e) {
        log.warn("WorkspaceException: {}", e.getMessage());
        BaseErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(ConquestException.class)
    public ResponseEntity<ApiResponse<Void>> handleConquestException(ConquestException e) {
        log.warn("ConquestException: {}", e.getMessage());
        BaseErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(SnsException.class)
    public ResponseEntity<ApiResponse<Void>> handleSnsException(SnsException e) {
        log.warn("SnsException: {}", e.getMessage());
        BaseErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserException(UserException e) {
        log.warn("UserException: {}", e.getMessage());
        BaseErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(errorCode));
    }

    // ── 멀티파트 파일 크기 초과 (Spring 레벨) ─────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("파일 크기 초과 (Spring multipart 제한): {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.FILE_TOO_LARGE.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(ErrorCode.FILE_TOO_LARGE.getMessage()));
    }

    // ── @Valid 유효성 검사 실패 ───────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidException(MethodArgumentNotValidException e,
                                                                   HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation 실패 [{} {}]: {}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(message));
    }

    // ── DB 제약 조건 위반 (중복 키 등) ──────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolation: {}", e.getMessage());
        return ResponseEntity
                .status(org.springframework.http.HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail("중복된 데이터입니다."));
    }

    // ── 그 외 예외 ───────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
