package com.double2and9.media.service.impl;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.GetPresignedUrlRequestDTO;
import com.double2and9.media.dto.GetPresignedUrlResponseDTO;
import com.double2and9.media.dto.InitiateMultipartUploadRequestDTO;
import com.double2and9.media.dto.InitiateMultipartUploadResponseDTO;
import com.double2and9.media.entity.MultipartUploadRecord;
import com.double2and9.media.repository.MultipartUploadRecordRepository;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class MediaUploadServiceImplTest {

    @Autowired
    private MediaUploadServiceImpl mediaUploadService;

    @Autowired
    private MultipartUploadRecordRepository multipartUploadRecordRepository;
    
    @Value("${minio.bucket-name}")
    private String defaultBucketName;
    
    @Value("${media.upload.chunk-size:5242880}")
    private Integer defaultChunkSize;

    @Autowired
    private MinioClient minioClient;

    @Test
    public void testInitiateMultipartUpload() {
        // 1. 准备测试数据
        InitiateMultipartUploadRequestDTO request = new InitiateMultipartUploadRequestDTO();
        request.setFileName("test-video.mp4");
        request.setFileSize(10L * 1024 * 1024); // 10MB
        request.setMediaType("VIDEO");
        request.setMimeType("video/mp4");
        request.setPurpose("TEST");
        request.setOrganizationId(1L);

        // 2. 调用测试方法
        InitiateMultipartUploadResponseDTO response = mediaUploadService.initiateMultipartUpload(request);

        // 3. 验证响应数据
        assertThat(response).isNotNull();
        assertThat(response.getUploadId()).isNotNull();
        assertThat(response.getMediaFileId()).isNotNull();
        assertThat(response.getBucket()).isEqualTo(defaultBucketName);
        assertThat(response.getFilePath()).contains("video/1/");
        assertThat(response.getChunkSize()).isEqualTo(defaultChunkSize);
        assertThat(response.getTotalChunks()).isEqualTo(
            (int) Math.ceil((double) request.getFileSize() / defaultChunkSize)
        );

        // 4. 验证数据库记录
        Optional<MultipartUploadRecord> record = multipartUploadRecordRepository
            .findByUploadId(response.getUploadId());
        
        assertThat(record).isPresent();
        MultipartUploadRecord savedRecord = record.get();
        assertThat(savedRecord.getMediaFileId()).isEqualTo(response.getMediaFileId());
        assertThat(savedRecord.getFileName()).isEqualTo(request.getFileName());
        assertThat(savedRecord.getFileSize()).isEqualTo(request.getFileSize());
        assertThat(savedRecord.getBucket()).isEqualTo(response.getBucket());
        assertThat(savedRecord.getFilePath()).isEqualTo(response.getFilePath());
        assertThat(savedRecord.getTotalChunks()).isEqualTo(response.getTotalChunks());
        assertThat(savedRecord.getChunkSize()).isEqualTo(response.getChunkSize());
        assertThat(savedRecord.getStatus()).isEqualTo("UPLOADING");
        assertThat(savedRecord.getUploadedChunks()).isZero();
        assertThat(savedRecord.getInitiateTime()).isNotNull();
        assertThat(savedRecord.getExpirationTime()).isNotNull();
    }

    @Test
    public void testGenerateMediaFileId() {
        // 1. 准备测试数据
        InitiateMultipartUploadRequestDTO request1 = new InitiateMultipartUploadRequestDTO();
        request1.setFileName("test1.mp4");
        request1.setFileSize(1024L);
        request1.setMediaType("VIDEO");
        request1.setOrganizationId(1L);

        InitiateMultipartUploadRequestDTO request2 = new InitiateMultipartUploadRequestDTO();
        request2.setFileName("test1.mp4"); // 相同文件名
        request2.setFileSize(1024L);
        request2.setMediaType("VIDEO");
        request2.setOrganizationId(1L);

        // 2. 调用测试方法
        InitiateMultipartUploadResponseDTO response1 = mediaUploadService.initiateMultipartUpload(request1);
        InitiateMultipartUploadResponseDTO response2 = mediaUploadService.initiateMultipartUpload(request2);

        // 3. 验证mediaFileId唯一性
        assertThat(response1.getMediaFileId()).isNotEqualTo(response2.getMediaFileId());
        assertThat(response1.getMediaFileId()).startsWith("org_1_");
        assertThat(response2.getMediaFileId()).startsWith("org_1_");
    }

    @Test
    public void testFilePath() {
        // 1. 准备测试数据
        InitiateMultipartUploadRequestDTO request = new InitiateMultipartUploadRequestDTO();
        request.setFileName("test-video.mp4");
        request.setFileSize(1024L);
        request.setMediaType("VIDEO");
        request.setOrganizationId(1L);

        // 2. 调用测试方法
        InitiateMultipartUploadResponseDTO response = mediaUploadService.initiateMultipartUpload(request);

        // 3. 验证文件路径格式
        String filePath = response.getFilePath();
        assertThat(filePath).startsWith("video/1/");
        assertThat(filePath).endsWith(".mp4");
        assertThat(filePath).contains(response.getMediaFileId());
    }

    @Test
    void testGetPresignedUrl() throws Exception {
        // 1. 先初始化一个分片上传会话
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024); // 10MB
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 准备获取预签名URL的请求
        GetPresignedUrlRequestDTO request = new GetPresignedUrlRequestDTO();
        request.setUploadId(initResponse.getUploadId());
        request.setChunkIndex(1);

        // 3. 获取预签名URL
        GetPresignedUrlResponseDTO response = mediaUploadService.getPresignedUrl(request);

        // 4. 验证响应
        assertThat(response).isNotNull();
        assertThat(response.getPresignedUrl()).isNotBlank();
        assertThat(response.getChunkIndex()).isEqualTo(1);
        assertThat(response.getExpirationTime()).isGreaterThan(System.currentTimeMillis());

        // 5. 验证预签名URL的格式
        URL url = new URL(response.getPresignedUrl());
        assertThat(url.getPath()).contains(initResponse.getFilePath() + ".part1");
        assertThat(url.getQuery()).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");

        // 6. 验证预签名URL是否可用
        try {
            // 使用MinIO SDK验证预签名URL
            StatObjectResponse statResponse = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(initResponse.getBucket())
                    .object(initResponse.getFilePath() + ".part1")
                    .build()
            );
            
            // 如果能获取到对象信息，说明预签名URL有效
            assertThat(statResponse).isNotNull();
        } catch (ErrorResponseException e) {
            // 预期会抛出 ErrorResponseException，因为对象还不存在
            assertThat(e.errorResponse().code()).isEqualTo("NoSuchKey");
        }
    }

    @Test
    void testGetPresignedUrlWithInvalidUploadId() {
        GetPresignedUrlRequestDTO request = new GetPresignedUrlRequestDTO();
        request.setUploadId("non-existent-upload-id");
        request.setChunkIndex(1);

        assertThatThrownBy(() -> mediaUploadService.getPresignedUrl(request))
                .isInstanceOf(MediaException.class)
                .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_SESSION_NOT_FOUND.getCode());
    }

    @Test
    void testGetPresignedUrlWithInvalidStatus() {
        // 1. 创建一个已完成的上传记录
        MultipartUploadRecord record = new MultipartUploadRecord();
        record.setUploadId("test-upload-id");
        record.setMediaFileId("test-media-id");
        record.setFileName("test.mp4");
        record.setBucket("test-bucket");
        record.setFilePath("video/1/test-media-id.mp4");
        record.setTotalChunks(3);
        record.setStatus("COMPLETED");
        multipartUploadRecordRepository.save(record);

        // 2. 尝试获取预签名URL
        GetPresignedUrlRequestDTO request = new GetPresignedUrlRequestDTO();
        request.setUploadId("test-upload-id");
        request.setChunkIndex(1);

        assertThatThrownBy(() -> mediaUploadService.getPresignedUrl(request))
                .isInstanceOf(MediaException.class)
                .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS.getCode());
    }

    @Test
    void testGetPresignedUrlWithInvalidChunkIndex() {
        // 1. 先初始化一个分片上传会话
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024); // 10MB
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 尝试获取无效分片索引的预签名URL
        GetPresignedUrlRequestDTO request = new GetPresignedUrlRequestDTO();
        request.setUploadId(initResponse.getUploadId());
        request.setChunkIndex(999); // 超出总分片数

        assertThatThrownBy(() -> mediaUploadService.getPresignedUrl(request))
                .isInstanceOf(MediaException.class)
                .hasFieldOrPropertyWithValue("code", MediaErrorCode.INVALID_CHUNK_INDEX.getCode());
    }

    @Test
    void testGetPresignedUrlWithDifferentChunks() throws Exception {
        // 1. 初始化分片上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(15L * 1024 * 1024); // 15MB，会产生3个分片
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 获取所有分片的预签名URL
        for (int i = 1; i <= initResponse.getTotalChunks(); i++) {
            GetPresignedUrlRequestDTO request = new GetPresignedUrlRequestDTO();
            request.setUploadId(initResponse.getUploadId());
            request.setChunkIndex(i);

            GetPresignedUrlResponseDTO response = mediaUploadService.getPresignedUrl(request);

            // 验证每个分片的预签名URL
            assertThat(response.getPresignedUrl()).isNotBlank();
            assertThat(response.getChunkIndex()).isEqualTo(i);
            
            URL url = new URL(response.getPresignedUrl());
            assertThat(url.getPath()).contains(String.format("%s.part%d", initResponse.getFilePath(), i));
            
            // 验证预签名URL的权限（PUT方法）
            assertThat(url.getQuery())
                .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
                .contains("X-Amz-SignedHeaders=host")
                .contains("X-Amz-Signature=");
        }
    }
} 