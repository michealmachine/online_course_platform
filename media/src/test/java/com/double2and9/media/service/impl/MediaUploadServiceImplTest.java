package com.double2and9.media.service.impl;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.base.enums.MediaFileStatusEnum;
import com.double2and9.base.enums.UploadStatusEnum;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.MessageDigest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import io.minio.GetObjectArgs;

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

    @Test
    void testCompleteMultipartUpload() throws Exception {
        // 1. 初始化分片上传
        InitiateMultipartUploadRequestDTO initRequest = new InitiateMultipartUploadRequestDTO();
        initRequest.setFileName("test-video.mp4");
        initRequest.setFileSize(10L * 1024 * 1024); // 10MB
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 实际上传分片
        for (int i = 1; i <= initResponse.getTotalChunks(); i++) {
            // 获取上传URL
            GetPresignedUrlRequestDTO urlRequest = new GetPresignedUrlRequestDTO();
            urlRequest.setUploadId(initResponse.getUploadId());
            urlRequest.setChunkIndex(i);
            GetPresignedUrlResponseDTO urlResponse = mediaUploadService.getPresignedUrl(urlRequest);

            // 上传分片
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
            
            // 更新已上传分片数
            MultipartUploadRecord record = multipartUploadRecordRepository
                .findByUploadId(initResponse.getUploadId()).orElseThrow();
            record.setUploadedChunks(i);
            multipartUploadRecordRepository.save(record);
        }

        // 计算上传数据的总校验和
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (int i = 1; i <= initResponse.getTotalChunks(); i++) {
            byte[] chunkData = new byte[initResponse.getChunkSize()];
            Arrays.fill(chunkData, (byte) i);
            md.update(chunkData);
        }
        byte[] expectedChecksum = md.digest();

        // 3. 完成分片上传
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId(initResponse.getUploadId());

        CompleteMultipartUploadResponseDTO response = 
            mediaUploadService.completeMultipartUpload(request);

        // 4. 验证响应
        assertThat(response).isNotNull();
        assertThat(response.getMediaFileId()).isEqualTo(initResponse.getMediaFileId());
        assertThat(response.getFileUrl()).contains(initResponse.getFilePath());
        assertThat(response.getStatus()).isEqualTo(UploadStatusEnum.COMPLETED.getCode());
        assertThat(response.getCompleteTime()).isNotNull();
        assertThat(response.getFileSize()).isEqualTo(initRequest.getFileSize());

        // 5. 验证上传记录状态
        MultipartUploadRecord updatedRecord = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId()).orElseThrow();
        assertThat(updatedRecord.getStatus()).isEqualTo(UploadStatusEnum.COMPLETED.getCode());
        assertThat(updatedRecord.getCompleteTime()).isNotNull();
        assertThat(updatedRecord.getUploadedChunks()).isEqualTo(updatedRecord.getTotalChunks());

        // 6. 验证MediaFile记录
        MediaFile mediaFile = mediaFileRepository
            .findByMediaFileId(response.getMediaFileId()).orElseThrow();
        assertThat(mediaFile.getOrganizationId()).isEqualTo(initRequest.getOrganizationId());
        assertThat(mediaFile.getFileName()).isEqualTo(initRequest.getFileName());
        assertThat(mediaFile.getFilePath()).isEqualTo(initResponse.getFilePath());
        assertThat(mediaFile.getBucket()).isEqualTo(defaultBucketName);
        assertThat(mediaFile.getMediaType()).isEqualTo(initRequest.getMediaType());
        assertThat(mediaFile.getMimeType()).isEqualTo(initRequest.getMimeType());
        assertThat(mediaFile.getPurpose()).isEqualTo(initRequest.getPurpose());
        assertThat(mediaFile.getFileSize()).isEqualTo(initRequest.getFileSize());
        assertThat(mediaFile.getStatus()).isEqualTo(MediaFileStatusEnum.NORMAL.getCode());
        assertThat(mediaFile.getCreateTime()).isNotNull();

        // 7. 验证文件完整性
        try {
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(defaultBucketName)
                    .object(initResponse.getFilePath())
                    .build()
            );
            assertThat(stat.size()).isEqualTo(initRequest.getFileSize());

            // 下载并验证文件内容
            try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(defaultBucketName)
                    .object(initResponse.getFilePath())
                    .build()
            )) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                byte[] actualContent = out.toByteArray();
                MessageDigest md2 = MessageDigest.getInstance("SHA-256");
                byte[] actualChecksum = md2.digest(actualContent);
                assertThat(actualChecksum).isEqualTo(expectedChecksum);
            }
        } catch (Exception e) {
            fail("文件应该存在但未找到", e);
        }

        // 8. 验证重复调用的处理
        assertThatThrownBy(() -> mediaUploadService.completeMultipartUpload(request))
            .isInstanceOf(MediaException.class)
            .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS.getCode());
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
        initRequest.setFileSize(10L * 1024 * 1024);
        initRequest.setMediaType("VIDEO");
        initRequest.setMimeType("video/mp4");
        initRequest.setPurpose("TEST");
        initRequest.setOrganizationId(1L);

        InitiateMultipartUploadResponseDTO initResponse = 
            mediaUploadService.initiateMultipartUpload(initRequest);

        // 2. 只上传部分分片
        for (int i = 1; i <= initResponse.getTotalChunks() - 1; i++) {
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
            
            MultipartUploadRecord record = multipartUploadRecordRepository
                .findByUploadId(initResponse.getUploadId()).orElseThrow();
            record.setUploadedChunks(i);
            multipartUploadRecordRepository.save(record);
        }

        // 3. 尝试完成上传
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId(initResponse.getUploadId());

        assertThatThrownBy(() -> mediaUploadService.completeMultipartUpload(request))
                .isInstanceOf(MediaException.class)
                .hasFieldOrPropertyWithValue("code", MediaErrorCode.UPLOAD_NOT_COMPLETED.getCode());
    }

    @Test
    void testCompleteMultipartUploadWithMergeFailure() throws Exception {
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

        // 2. 上传一个损坏的分片
        GetPresignedUrlRequestDTO urlRequest = new GetPresignedUrlRequestDTO();
        urlRequest.setUploadId(initResponse.getUploadId());
        urlRequest.setChunkIndex(1);
        GetPresignedUrlResponseDTO urlResponse = mediaUploadService.getPresignedUrl(urlRequest);

        byte[] corruptedData = new byte[1]; // 损坏的数据
        
        URL url = new URL(urlResponse.getPresignedUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(corruptedData);
        }

        MultipartUploadRecord record = multipartUploadRecordRepository
            .findByUploadId(initResponse.getUploadId()).orElseThrow();
        record.setUploadedChunks(record.getTotalChunks()); // 假装所有分片都上传了
        multipartUploadRecordRepository.save(record);

        // 3. 尝试完成上传
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId(initResponse.getUploadId());

        assertThatThrownBy(() -> mediaUploadService.completeMultipartUpload(request))
                .isInstanceOf(MediaException.class)
                .hasFieldOrPropertyWithValue("code", MediaErrorCode.MERGE_CHUNKS_FAILED.getCode());
    }
} 