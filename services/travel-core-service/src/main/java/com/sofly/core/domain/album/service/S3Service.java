package com.sofly.core.domain.album.service;

import com.sofly.core.global.config.S3Config;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;

    /** 업로드용 Presigned PUT URL 발급 (10분) */
    public String generatePresignedUploadUrl(String s3Key, String contentType) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getS3().getBucket())
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(putRequest)
                    .build();

            return s3Presigner.presignPutObject(presignRequest).url().toString();
        } catch (SdkException e) {
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
            throw new SoflyException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    /** S3 URL 생성 (업로드 완료 후 저장할 public URL) */
    public String buildObjectUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Config.getS3().getBucket(),
                s3Config.getS3().getRegion(),
                s3Key);
    }
}
