package com.sofly.core.domain.album.service;

import com.sofly.core.global.config.S3Config;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;

    /** 파일을 S3에 업로드하고 URL 반환 */
    public String uploadFile(MultipartFile file, String s3Key) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getS3().getBucket())
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return buildObjectUrl(s3Key);
        } catch (IOException | SdkException e) {
            log.error("S3 업로드 실패. key={}, error={}", s3Key, e.getMessage(), e);
            throw new SoflyException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    /** 다운로드용 Presigned GET URL 발급 (5분) */
    public String generatePresignedDownloadUrl(String s3Key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(s3Config.getS3().getBucket())
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(5))
                    .getObjectRequest(getRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException e) {
            log.error("S3 Presigned URL 생성 실패. key={}, error={}", s3Key, e.getMessage(), e);
            throw new SoflyException(ErrorCode.S3_DOWNLOAD_FAILED);
        }
    }

    /** S3 객체 삭제 */
    public void deleteObject(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Config.getS3().getBucket())
                    .key(s3Key)
                    .build());
        } catch (NoSuchKeyException e) {
            log.warn("S3 객체가 이미 존재하지 않습니다: {}", s3Key);
        } catch (SdkException e) {
            log.error("S3 삭제 실패. key={}, error={}", s3Key, e.getMessage(), e);
            throw new SoflyException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    /** S3 객체 URL 생성 */
    public String buildObjectUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Config.getS3().getBucket(),
                s3Config.getS3().getRegion(),
                s3Key);
    }
}
