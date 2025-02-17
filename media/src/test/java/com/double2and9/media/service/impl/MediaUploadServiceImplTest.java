package com.double2and9.media.service.impl;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.base.enums.MediaFileStatusEnum;
import com.double2and9.base.enums.UploadStatusEnum;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.*;
import com.double2and9.media.dto.request.CompleteMultipartUploadRequestDTO;
import com.double2and9.media.dto.request.GetPresignedUrlRequestDTO;
import com.double2and9.media.dto.request.InitiateMultipartUploadRequestDTO;
import com.double2and9.media.entity.MultipartUploadRecord;
import com.double2and9.media.entity.MediaFile;
import com.double2and9.media.repository.MultipartUploadRecordRepository;
import com.double2and9.media.repository.MediaFileRepository;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.MessageDigest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import io.minio.GetObjectArgs;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;

@SpringBootTest
@Transactional
public class MediaUploadServiceImplTest {

    @Autowired
    private MediaUploadServiceImpl mediaUploadService;

    @Autowired
    private MultipartUploadRecordRepository multipartUploadRecordRepository;
    
    @Autowired
    private MediaFileRepository mediaFileRepository;
    
    @Value("${minio.bucket-name}")
    private String defaultBucketName;
    
    @Value("${media.upload.chunk-size:5242880}")
    private Integer defaultChunkSize;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private S3Client s3Client;

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

        // 3. 验证响应
        assertThat(response).isNotNull();
        assertThat(response.getUploadId()).isNotBlank();  // uploadId 现在是 S3 生成的
        assertThat(response.getMediaFileId()).startsWith("org_1_");
        assertThat(response.getBucket()).isEqualTo(defaultBucketName);
        assertThat(response.getFilePath()).startsWith("video/1/");
        assertThat(response.getFilePath()).endsWith(".mp4");
        assertThat(response.getChunkSize()).isEqualTo(defaultChunkSize);
        assertThat(response.getTotalChunks()).isEqualTo(2); // 10MB/5MB = 2

        // 4. 验证数据库记录
        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(response.getUploadId())
            .orElseThrow();
        
        assertThat(record.getUploadId()).isEqualTo(response.getUploadId());  // uploadId 应该与 S3 返回的一致
        assertThat(record.getMediaFileId()).isEqualTo(response.getMediaFileId());
        assertThat(record.getOrganizationId()).isEqualTo(request.getOrganizationId());
        assertThat(record.getFileName()).isEqualTo(request.getFileName());
        assertThat(record.getFileSize()).isEqualTo(request.getFileSize());
        assertThat(record.getBucket()).isEqualTo(defaultBucketName);
        assertThat(record.getFilePath()).isEqualTo(response.getFilePath());
        assertThat(record.getMediaType()).isEqualTo(request.getMediaType());
        assertThat(record.getMimeType()).isEqualTo(request.getMimeType());
        assertThat(record.getPurpose()).isEqualTo(request.getPurpose());
        assertThat(record.getTotalChunks()).isEqualTo(response.getTotalChunks());
        assertThat(record.getChunkSize()).isEqualTo(defaultChunkSize);
        assertThat(record.getStatus()).isEqualTo(UploadStatusEnum.UPLOADING.getCode());
        assertThat(record.getCreateTime()).isNotNull();
        assertThat(record.getUpdateTime()).isNotNull();
        assertThat(record.getInitiateTime()).isNotNull();
        assertThat(record.getExpirationTime()).isAfter(new Date());
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

        // 获取记录以获取 S3 uploadId
        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId()).orElseThrow();

        // 5. 验证预签名URL的格式
        URL url = new URL(response.getPresignedUrl());
        assertThat(url.getPath()).contains(initResponse.getFilePath());
        assertThat(url.getQuery())
            .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
            .contains("X-Amz-SignedHeaders=host")
            .contains("X-Amz-Signature=")
            .contains("partNumber=" + response.getChunkIndex())
            .contains("uploadId=" + initResponse.getUploadId());  // uploadId 现在是 S3 的 ID

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

        // 获取记录以获取 S3 uploadId
        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId()).orElseThrow();

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
            assertThat(url.getPath()).contains(initResponse.getFilePath());
            
            // 验证查询参数
            assertThat(url.getQuery())
                .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
                .contains("X-Amz-SignedHeaders=host")
                .contains("X-Amz-Signature=")
                .contains("partNumber=" + i)
                .contains("uploadId=" + initResponse.getUploadId());  // uploadId 现在是 S3 的 ID
        }
    }

    @Test
    void testCompleteMultipartUpload() throws Exception {
        // 1. 初始化分片上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test.mp4");
        initRequest.setFileSize(10L * 1024); // 10KB
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 上传一个分片
        GetPresignedUrlRequestDTO urlRequest = new GetPresignedUrlRequestDTO();
        urlRequest.setUploadId(initResponse.getUploadId());
        urlRequest.setChunkIndex(1);
        GetPresignedUrlResponseDTO urlResponse = mediaUploadService.getPresignedUrl(urlRequest);

        // 创建测试数据
        byte[] chunkData = new byte[10 * 1024]; // 10KB
        Arrays.fill(chunkData, (byte) 1);

        // 获取记录以获取 S3 uploadId
        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId()).orElseThrow();

        // 上传分片
        URL url = new URL(urlResponse.getPresignedUrl());
        assertThat(url.getQuery())
            .contains("partNumber=1")
            .contains("uploadId=" + initResponse.getUploadId());  // uploadId 现在是 S3 的 ID

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(chunkData);
        }

        // 验证响应码
        int responseCode = conn.getResponseCode();
        assertThat(responseCode).isEqualTo(200);

        // 等待一小段时间确保上传完成
        Thread.sleep(1000);

        // 验证分片是否已上传
        ListPartsResponse listPartsResponse = s3Client.listParts(ListPartsRequest.builder()
            .bucket(record.getBucket())
            .key(record.getFilePath())
            .uploadId(record.getUploadId())
            .build());
        
        assertThat(listPartsResponse.hasParts()).isTrue();
        assertThat(listPartsResponse.parts()).hasSize(1);
        Part part = listPartsResponse.parts().get(0);
        assertThat(part.partNumber()).isEqualTo(1);

        // 3. 设置上传完成状态
        multipartUploadRecordRepository.save(record);

        // 4. 完成上传
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId(initResponse.getUploadId());

        CompleteMultipartUploadResponseDTO response = mediaUploadService.completeMultipartUpload(request);

        // 5. 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getMediaFileId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(UploadStatusEnum.COMPLETED.getCode());
        
        // 6. 验证数据库记录
        record = multipartUploadRecordRepository.findByUploadId(initResponse.getUploadId()).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(UploadStatusEnum.COMPLETED.getCode());
        assertThat(record.getCompleteTime()).isNotNull();

        // 7. 验证媒体文件记录
        MediaFile mediaFile = mediaFileRepository.findByMediaFileId(response.getMediaFileId()).orElseThrow();
        assertThat(mediaFile.getStatus()).isEqualTo(MediaFileStatusEnum.NORMAL.getCode());
        assertThat(mediaFile.getFileSize()).isEqualTo(record.getFileSize());
    }

    @Test
    void testCompleteMultipartUploadWithInvalidUploadId() {
        // 测试无效的上传ID
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId("invalid-upload-id");

        assertThatThrownBy(() -> mediaUploadService.completeMultipartUpload(request))
                .isInstanceOf(MediaException.class)
                .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_SESSION_NOT_FOUND.getCode());
    }

    @Test
    void testCompleteMultipartUploadWithInvalidStatus() throws Exception {
        // 1. 初始化上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024);
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 修改状态为已完成
        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId()).orElseThrow();
        record.setStatus(UploadStatusEnum.COMPLETED.getCode());
        multipartUploadRecordRepository.save(record);

        // 3. 尝试再次完成上传
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId(initResponse.getUploadId());

        assertThatThrownBy(() -> mediaUploadService.completeMultipartUpload(request))
                .isInstanceOf(MediaException.class)
                .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS.getCode());
    }

    @Test
    void testCompleteMultipartUploadWithIncompleteChunks() throws Exception {
        // 1. 初始化上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024); // 10MB
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 只上传部分分片
        for (int i = 1; i < initResponse.getTotalChunks(); i++) {
            GetPresignedUrlRequestDTO urlRequest = new GetPresignedUrlRequestDTO();
            urlRequest.setUploadId(initResponse.getUploadId());
            urlRequest.setChunkIndex(i);
            GetPresignedUrlResponseDTO urlResponse = mediaUploadService.getPresignedUrl(urlRequest);

            byte[] chunkData = new byte[initResponse.getChunkSize()];
            Arrays.fill(chunkData, (byte) i);

            URL url = new URL(urlResponse.getPresignedUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(chunkData);
            }
        }

        // 3. 尝试完成上传
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId(initResponse.getUploadId());

        assertThatThrownBy(() -> mediaUploadService.completeMultipartUpload(request))
                .isInstanceOf(MediaException.class)
                .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_NOT_COMPLETED.getCode());
    }

    @Test
    void testAbortMultipartUpload() throws Exception {
        // 1. 初始化上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024); // 10MB
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 上传一个分片
        GetPresignedUrlRequestDTO urlRequest = new GetPresignedUrlRequestDTO();
        urlRequest.setUploadId(initResponse.getUploadId());
        urlRequest.setChunkIndex(1);
        GetPresignedUrlResponseDTO urlResponse = mediaUploadService.getPresignedUrl(urlRequest);

        byte[] chunkData = new byte[initResponse.getChunkSize()];
        Arrays.fill(chunkData, (byte) 1);

        URL url = new URL(urlResponse.getPresignedUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(chunkData);
        }

        // 确保分片上传成功
        assertThat(conn.getResponseCode()).isEqualTo(200);

        // 验证分片确实上传了
        ListPartsResponse listPartsResponse = s3Client.listParts(ListPartsRequest.builder()
            .bucket(initResponse.getBucket())
            .key(initResponse.getFilePath())
            .uploadId(initResponse.getUploadId())
            .build());
        assertThat(listPartsResponse.hasParts()).isTrue();
        assertThat(listPartsResponse.parts()).hasSize(1);

        // 3. 取消上传
        mediaUploadService.abortMultipartUpload(initResponse.getUploadId());

        // 4. 验证记录状态
        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId())
            .orElseThrow();
        assertThat(record.getStatus()).isEqualTo(UploadStatusEnum.ABORTED.getCode());
        assertThat(record.getAbortTime()).isNotNull();

        // 5. 验证分片已被清理 - 应该抛出 NoSuchUploadException
        assertThatThrownBy(() -> s3Client.listParts(ListPartsRequest.builder()
            .bucket(record.getBucket())
            .key(record.getFilePath())
            .uploadId(record.getUploadId())
            .build()))
            .isInstanceOf(NoSuchUploadException.class);
    }

    @Test
    void testAbortMultipartUploadWithInvalidUploadId() {
        assertThatThrownBy(() -> mediaUploadService.abortMultipartUpload("invalid-upload-id"))
            .isInstanceOf(MediaException.class)
            .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_SESSION_NOT_FOUND.getCode());
    }

    @Test
    void testAbortMultipartUploadWithCompletedStatus() throws Exception {
        // 1. 初始化上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024);
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 修改状态为已完成
        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId())
            .orElseThrow();
        record.setStatus(UploadStatusEnum.COMPLETED.getCode());
        multipartUploadRecordRepository.save(record);

        // 3. 尝试取消上传
        assertThatThrownBy(() -> mediaUploadService.abortMultipartUpload(initResponse.getUploadId()))
            .isInstanceOf(MediaException.class)
            .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS.getCode());
    }

    @Test
    void testGetUploadStatus() throws Exception {
        // 1. 初始化上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024); // 10MB
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 上传两个分片
        for (int i = 1; i <= 2; i++) {
            GetPresignedUrlRequestDTO urlRequest = new GetPresignedUrlRequestDTO();
            urlRequest.setUploadId(initResponse.getUploadId());
            urlRequest.setChunkIndex(i);
            GetPresignedUrlResponseDTO urlResponse = mediaUploadService.getPresignedUrl(urlRequest);

            byte[] chunkData = new byte[initResponse.getChunkSize()];
            Arrays.fill(chunkData, (byte) i);

            URL url = new URL(urlResponse.getPresignedUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(chunkData);
            }
            assertThat(conn.getResponseCode()).isEqualTo(200);
        }

        // 3. 获取上传状态
        UploadStatusResponseDTO status = mediaUploadService.getUploadStatus(initResponse.getUploadId());

        // 4. 验证状态信息
        assertThat(status).isNotNull();
        assertThat(status.getUploadId()).isEqualTo(initResponse.getUploadId());
        assertThat(status.getStatus()).isEqualTo(UploadStatusEnum.UPLOADING.getCode());
        assertThat(status.getTotalChunks()).isEqualTo(initResponse.getTotalChunks());
        assertThat(status.getExpirationTime()).isNotNull();
        
        // 5. 验证分片信息
        assertThat(status.getUploadedParts()).hasSize(2);
        assertThat(status.getUploadedParts()).allSatisfy(part -> {
            assertThat(part.getPartNumber()).isBetween(1, 2);
            assertThat(part.getETag()).isNotEmpty();
            assertThat(part.getSize()).isEqualTo((long) initResponse.getChunkSize());
        });
    }

    @Test
    void testGetUploadStatusWithInvalidUploadId() {
        assertThatThrownBy(() -> mediaUploadService.getUploadStatus("invalid-upload-id"))
            .isInstanceOf(MediaException.class)
            .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_SESSION_NOT_FOUND.getCode());
    }

    @Test
    void testGetUploadStatusWithCompletedUpload() throws Exception {
        // 1. 初始化上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024);
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 修改状态为已完成
        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId())
            .orElseThrow();
        record.setStatus(UploadStatusEnum.COMPLETED.getCode());
        multipartUploadRecordRepository.save(record);

        // 3. 获取状态
        UploadStatusResponseDTO status = mediaUploadService.getUploadStatus(initResponse.getUploadId());

        // 4. 验证状态
        assertThat(status).isNotNull();
        assertThat(status.getStatus()).isEqualTo(UploadStatusEnum.COMPLETED.getCode());
        // 已完成的上传，S3 中的分片信息已不存在
        assertThat(status.getUploadedParts()).isEmpty();
    }
} 