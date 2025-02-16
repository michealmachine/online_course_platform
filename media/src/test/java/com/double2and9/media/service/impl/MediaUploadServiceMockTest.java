package com.double2and9.media.service.impl;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.base.enums.UploadStatusEnum;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.*;
import com.double2and9.media.entity.MediaFile;
import com.double2and9.media.entity.MultipartUploadRecord;
import com.double2and9.media.repository.MediaFileRepository;
import com.double2and9.media.repository.MultipartUploadRecordRepository;
import io.minio.ComposeObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.ObjectWriteResponse;

import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class MediaUploadServiceMockTest {

    @Autowired
    private MediaUploadServiceImpl mediaUploadService;

    @Autowired
    private MultipartUploadRecordRepository multipartUploadRecordRepository;

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private MediaFileRepository mediaFileRepository;

    @Value("${minio.bucket-name}")
    private String defaultBucketName;

    @Test
    void testMinioComposeObjectFailure() throws Exception {
        // 1. 准备测试数据
        MultipartUploadRecord record = new MultipartUploadRecord();
        record.setUploadId("test-upload-id");
        record.setMediaFileId("test-media-id");
        record.setOrganizationId(1L);
        record.setFileName("test.mp4");
        record.setFileSize(1024L);
        record.setBucket(defaultBucketName);
        record.setFilePath("test/path.mp4");
        record.setMediaType("VIDEO");
        record.setMimeType("video/mp4");
        record.setPurpose("TEST");
        record.setTotalChunks(2);
        record.setUploadedChunks(2);
        record.setStatus(UploadStatusEnum.UPLOADING.getCode());
        multipartUploadRecordRepository.save(record);

        // 2. Mock MinIO抛出异常
        ErrorResponse errorResponse = new ErrorResponse(
            "NoSuchKey",                    // code
            "Object does not exist",        // message
            defaultBucketName,              // bucketName
            "test/path.mp4.part1",         // objectName
            "/test/path.mp4.part1",        // resource
            "test-request-id",             // requestId
            "test-host-id"                 // hostId
        );
        
        doThrow(new ErrorResponseException(
            errorResponse,
            null,
            "HTTP/1.1 404 Not Found"
        )).when(minioClient).composeObject(any(ComposeObjectArgs.class));

        // 3. 执行测试
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId("test-upload-id");

        assertThatThrownBy(() -> mediaUploadService.completeMultipartUpload(request))
            .isInstanceOf(MediaException.class)
            .hasFieldOrPropertyWithValue("code", MediaErrorCode.MERGE_CHUNKS_FAILED.getCode());

        // 4. 验证状态未变更
        MultipartUploadRecord updatedRecord = multipartUploadRecordRepository
            .findByUploadId("test-upload-id").orElseThrow();
        assertThat(updatedRecord.getStatus()).isEqualTo(UploadStatusEnum.UPLOADING.getCode());
    }

    @Test
    void testMinioStatObjectFailure() throws Exception {
        // 1. 准备测试数据
        MultipartUploadRecord record = new MultipartUploadRecord();
        record.setUploadId("test-upload-id");
        record.setMediaFileId("test-media-id");
        record.setOrganizationId(1L);
        record.setFileName("test.mp4");
        record.setFileSize(1024L);
        record.setBucket(defaultBucketName);
        record.setFilePath("test/path.mp4");
        record.setMediaType("VIDEO");
        record.setMimeType("video/mp4");
        record.setPurpose("TEST");
        record.setTotalChunks(2);
        record.setUploadedChunks(2);
        record.setStatus(UploadStatusEnum.UPLOADING.getCode());
        multipartUploadRecordRepository.save(record);

        // 2. Mock MinIO方法
        ObjectWriteResponse mockWriteResponse = mock(ObjectWriteResponse.class);
        when(minioClient.composeObject(any(ComposeObjectArgs.class)))
            .thenReturn(mockWriteResponse);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "NoSuchKey",
            "Object does not exist",
            defaultBucketName,
            "test/path.mp4",
            "/test/path.mp4",
            "test-request-id",
            "test-host-id"
        );

        when(minioClient.statObject(any(StatObjectArgs.class)))
            .thenThrow(new ErrorResponseException(
                errorResponse,
                null,
                "HTTP/1.1 404 Not Found"
            ));

        // 3. 执行测试
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId("test-upload-id");

        CompleteMultipartUploadResponseDTO response = 
            mediaUploadService.completeMultipartUpload(request);

        // 4. 验证使用了上传记录中的文件大小
        ArgumentCaptor<MediaFile> mediaFileCaptor = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFileRepository).save(mediaFileCaptor.capture());
        MediaFile savedMediaFile = mediaFileCaptor.getValue();
        assertThat(savedMediaFile.getFileSize()).isEqualTo(record.getFileSize());
    }

    @Test
    void testMediaFileRepositorySaveFailure() throws Exception {
        // 1. 准备测试数据
        MultipartUploadRecord record = new MultipartUploadRecord();
        record.setUploadId("test-upload-id");
        record.setMediaFileId("test-media-id");
        record.setOrganizationId(1L);
        record.setFileName("test.mp4");
        record.setFileSize(1024L);
        record.setBucket(defaultBucketName);
        record.setFilePath("test/path.mp4");
        record.setMediaType("VIDEO");
        record.setMimeType("video/mp4");
        record.setPurpose("TEST");
        record.setTotalChunks(2);
        record.setUploadedChunks(2);
        record.setStatus(UploadStatusEnum.UPLOADING.getCode());
        multipartUploadRecordRepository.save(record);

        // 2. Mock MinIO和Repository方法
        ObjectWriteResponse mockWriteResponse = mock(ObjectWriteResponse.class);
        when(minioClient.composeObject(any(ComposeObjectArgs.class)))
            .thenReturn(mockWriteResponse);
        
        StatObjectResponse mockResponse = mock(StatObjectResponse.class);
        when(mockResponse.size()).thenReturn(1024L);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(mockResponse);
        
        when(mediaFileRepository.save(any(MediaFile.class)))
            .thenThrow(new RuntimeException("Database error"));

        // 3. 执行测试
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId("test-upload-id");

        assertThatThrownBy(() -> mediaUploadService.completeMultipartUpload(request))
            .isInstanceOf(MediaException.class)
            .hasFieldOrPropertyWithValue("code", MediaErrorCode.CREATE_MEDIA_FILE_FAILED.getCode());

        // 4. 验证状态未变更
        MultipartUploadRecord updatedRecord = multipartUploadRecordRepository
            .findByUploadId("test-upload-id").orElseThrow();
        assertThat(updatedRecord.getStatus()).isEqualTo(UploadStatusEnum.UPLOADING.getCode());
    }
} 