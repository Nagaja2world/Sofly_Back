package com.sofly.core.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@ConfigurationProperties(prefix = "cloud.aws")
@Getter
@Setter
public class S3Config {

    private S3Properties s3 = new S3Properties();
    private CredentialsProperties credentials = new CredentialsProperties();

    @Getter
    @Setter
    public static class S3Properties {
        private String bucket;
        private String region;
        // 로컬 개발 시 LocalStack 등 엔드포인트 오버라이드용 (미설정 시 실제 AWS 사용)
        private String endpoint;
    }

    @Getter
    @Setter
    public static class CredentialsProperties {
        private String accessKey;
        private String secretKey;
    }

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(credentialsProvider());

        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
            // LocalStack은 path-style URL 필요 (버킷명이 호스트 대신 경로에 위치)
            builder.endpointOverride(URI.create(s3.getEndpoint()))
                   .forcePathStyle(true);
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(credentialsProvider());

        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.getEndpoint()));
        }

        return builder.build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(credentials.getAccessKey(), credentials.getSecretKey())
        );
    }
}
