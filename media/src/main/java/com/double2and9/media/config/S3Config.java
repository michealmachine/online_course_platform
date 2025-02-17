package com.double2and9.media.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.net.URI;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Data
public class S3Config {
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private boolean pathStyleAccess;

    @Bean
    public S3Client s3Client() {
        AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        S3Configuration serviceConfiguration = S3Configuration.builder()
            .pathStyleAccessEnabled(pathStyleAccess)
            .chunkedEncodingEnabled(true)
            .build();

        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(() -> credentials)
            .region(Region.of(region))
            .serviceConfiguration(serviceConfiguration)
            .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        S3Configuration serviceConfiguration = S3Configuration.builder()
            .pathStyleAccessEnabled(pathStyleAccess)
            .build();

        return S3Presigner.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(() -> credentials)
            .region(Region.of(region))
            .serviceConfiguration(serviceConfiguration)
            .build();
    }
} 