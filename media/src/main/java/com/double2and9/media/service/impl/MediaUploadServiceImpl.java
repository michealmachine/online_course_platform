package com.double2and9.media.service.impl;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.base.enums.UploadStatusEnum;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.*;
import com.double2and9.media.dto.request.CompleteMultipartUploadRequestDTO;
import com.double2and9.media.dto.request.GetPresignedUrlRequestDTO;
import com.double2and9.media.dto.request.InitiateMultipartUploadRequestDTO;
import com.double2and9.media.dto.request.UploadedPartDTO;
import com.double2and9.media.entity.MultipartUploadRecord;
import com.double2and9.media.repository.MultipartUploadRecordRepository;
import com.double2and9.media.repository.MediaFileRepository;
import com.double2and9.media.service.MediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

import com.double2and9.base.enums.MediaFileStatusEnum;
import com.double2and9.media.entity.MediaFile;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Duration;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadServiceImpl implements MediaUploadService {

    private final MultipartUploadRecordRepository multipartUploadRecordRepository;
    private final MediaFileRepository mediaFileRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    
    @Value("${minio.bucket-name}")
    private String defaultBucketName;
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    @Value("${media.upload.chunk-size:5242880}")  // 默认5MB
    private Integer defaultChunkSize;
    
    @Value("${media.upload.expiration-hours:24}")  // 默认24小时
    private Integer expirationHours;

    @Value("${media.upload.presigned-url.expiration-seconds:3600}")  // 默认1小时
    private Integer presignedUrlExpirationSeconds;

    @Override
    @Transactional
    public InitiateMultipartUploadResponseDTO initiateMultipartUpload(
            InitiateMultipartUploadRequestDTO request) {
        
        // 1. 生成唯一标识
        String mediaFileId = generateMediaFileId(request.getOrganizationId(), request.getFileName());
        
        // 2. 生成存储路径
        String extension = getFileExtension(request.getFileName());
        String filePath = generateFilePath(
            request.getOrganizationId(),
            request.getMediaType(),
            mediaFileId,
            extension
        );
        
        // 3. 计算分片信息
        int chunkSize = defaultChunkSize;
        int totalChunks = calculateTotalChunks(request.getFileSize(), chunkSize);
        
        // 初始化S3分片上传
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(defaultBucketName)
                .key(filePath)
                .contentType(request.getMimeType())
                .build();
                
        CreateMultipartUploadResponse s3Response = s3Client.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = s3Response.uploadId();  // 直接使用S3的uploadId
        
        // 4. 创建上传记录
        MultipartUploadRecord record = new MultipartUploadRecord();
        record.setUploadId(uploadId);  // 使用S3的uploadId作为主键
        record.setMediaFileId(mediaFileId);
        record.setOrganizationId(request.getOrganizationId());
        record.setFileName(request.getFileName());
        record.setFileSize(request.getFileSize());
        record.setBucket(defaultBucketName);
        record.setFilePath(filePath);
        record.setMediaType(request.getMediaType());
        record.setMimeType(request.getMimeType());
        record.setPurpose(request.getPurpose());
        record.setTotalChunks(totalChunks);
        record.setChunkSize(chunkSize);
        record.setStatus(UploadStatusEnum.UPLOADING.getCode());
        record.setInitiateTime(new Date());
        record.setExpirationTime(calculateExpirationTime());
        
        multipartUploadRecordRepository.save(record);
        
        // 5. 构建响应
        return InitiateMultipartUploadResponseDTO.builder()
                .uploadId(uploadId)
                .mediaFileId(mediaFileId)
                .bucket(defaultBucketName)
                .filePath(filePath)
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .build();
    }
    
    /**
     * 生成媒体文件ID
     */
    private String generateMediaFileId(Long organizationId, String fileName) {
        return String.format("org_%d_%s", 
            organizationId, 
            UUID.randomUUID().toString().replace("-", "")
        );
    }
    
    /**
     * 生成文件存储路径
     */
    private String generateFilePath(Long organizationId, String mediaType, String mediaFileId, String extension) {
        return String.format("%s/%d/%s%s",
            mediaType.toLowerCase(),
            organizationId,
            mediaFileId,
            extension
        );
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex);
    }
    
    /**
     * 计算总分片数
     */
    private int calculateTotalChunks(Long fileSize, int chunkSize) {
        return (int) Math.ceil((double) fileSize / chunkSize);
    }
    
    /**
     * 计算过期时间
     */
    private Date calculateExpirationTime() {
        return new Date(System.currentTimeMillis() + expirationHours * 3600 * 1000);
    }

    @Override
    public GetPresignedUrlResponseDTO getPresignedUrl(GetPresignedUrlRequestDTO request) {
        log.debug("开始处理获取预签名URL请求: request={}", request);
        
        // 1. 查询上传记录
        MultipartUploadRecord record = multipartUploadRecordRepository.findByUploadId(request.getUploadId())
                .orElseThrow(() -> new MediaException(MediaErrorCode.UPLOAD_SESSION_NOT_FOUND));
        
        // 2. 验证上传状态
        if (!"UPLOADING".equals(record.getStatus())) {
            throw new MediaException(MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS);
        }
        
        // 3. 验证分片索引
        if (request.getChunkIndex() > record.getTotalChunks()) {
            throw new MediaException(MediaErrorCode.INVALID_CHUNK_INDEX);
        }
        
        try {
            String presignedUrl = s3Presigner.presignUploadPart(builder -> builder
                    .uploadPartRequest(req -> req
                            .bucket(record.getBucket())
                            .key(record.getFilePath())
                            .uploadId(record.getUploadId())
                            .partNumber(request.getChunkIndex())
                            .build())
                    .signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
            ).url().toString();

            return GetPresignedUrlResponseDTO.builder()
                    .presignedUrl(presignedUrl)
                    .chunkIndex(request.getChunkIndex())
                    .expirationTime(System.currentTimeMillis() + (presignedUrlExpirationSeconds * 1000L))
                    .build();
        } catch (Exception e) {
            log.error("生成预签名URL失败", e);
            throw new MediaException(MediaErrorCode.GENERATE_PRESIGNED_URL_FAILED);
        }
    }

    @Override
    @Transactional
    public CompleteMultipartUploadResponseDTO completeMultipartUpload(CompleteMultipartUploadRequestDTO request) {
        // 获取上传记录
        MultipartUploadRecord record = multipartUploadRecordRepository.findByUploadId(request.getUploadId())
            .orElseThrow(() -> new MediaException(MediaErrorCode.UPLOAD_SESSION_NOT_FOUND));

        // 检查状态
        if (UploadStatusEnum.COMPLETED.getCode().equals(record.getStatus())) {
            throw new MediaException(MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS);
        }

        try {
            // 获取所有已上传分片的信息
            ListPartsResponse listPartsResponse = s3Client.listParts(ListPartsRequest.builder()
                .bucket(record.getBucket())
                .key(record.getFilePath())
                .uploadId(record.getUploadId())
                .build());

            // 检查分片是否都已上传
            if (listPartsResponse.parts().size() < record.getTotalChunks()) {
                throw new MediaException(MediaErrorCode.UPLOAD_NOT_COMPLETED);
            }

            // 按分片号排序并构建完成请求
            List<CompletedPart> completedParts = listPartsResponse.parts().stream()
                .map(part -> CompletedPart.builder()
                    .partNumber(part.partNumber())
                    .eTag(part.eTag())
                    .build())
                .collect(Collectors.toList());

            // 完成分片上传
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(record.getBucket())
                .key(record.getFilePath())
                .uploadId(record.getUploadId())
                .multipartUpload(CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build())
                .build());

            // 更新记录状态
            record.setStatus(UploadStatusEnum.COMPLETED.getCode());
            record.setCompleteTime(new Date());
            multipartUploadRecordRepository.save(record);

            // 创建媒体文件记录
            MediaFile mediaFile = createMediaFile(record);
            mediaFileRepository.save(mediaFile);

            return buildCompleteResponse(record, mediaFile);
        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            throw new MediaException(MediaErrorCode.MERGE_CHUNKS_FAILED);
        }
    }

    private MediaFile createMediaFile(MultipartUploadRecord record) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setOrganizationId(record.getOrganizationId());
        mediaFile.setMediaFileId(record.getMediaFileId());
        mediaFile.setFileName(record.getFileName());
        mediaFile.setFilePath(record.getFilePath());
        mediaFile.setBucket(record.getBucket());
        mediaFile.setMediaType(record.getMediaType());
        mediaFile.setMimeType(record.getMimeType());
        mediaFile.setPurpose(record.getPurpose());
        mediaFile.setCreateTime(new Date());
        mediaFile.setStatus(MediaFileStatusEnum.NORMAL.getCode());
        
        try {
            HeadObjectResponse response = s3Client.headObject(req -> req
                    .bucket(record.getBucket())
                    .key(record.getFilePath())
            );
            mediaFile.setFileSize(response.contentLength());
        } catch (Exception e) {
            log.error("获取文件大小失败", e);
            mediaFile.setFileSize(record.getFileSize());
        }
        
        return mediaFile;
    }

    private CompleteMultipartUploadResponseDTO buildCompleteResponse(MultipartUploadRecord record, MediaFile mediaFile) {
        return CompleteMultipartUploadResponseDTO.builder()
                .mediaFileId(record.getMediaFileId())
                .fileUrl(String.format("%s/%s/%s", minioEndpoint, record.getBucket(), record.getFilePath()))
                .fileSize(mediaFile.getFileSize())
                .status(UploadStatusEnum.COMPLETED.getCode())
                .completeTime(record.getCompleteTime().getTime())
                .build();
    }

    @Override
    @Transactional
    public void abortMultipartUpload(String uploadId) {
        // 1. 获取上传记录
        MultipartUploadRecord record = multipartUploadRecordRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new MediaException(MediaErrorCode.UPLOAD_SESSION_NOT_FOUND));

        // 2. 检查状态 - 只有上传中的才能取消
        if (!UploadStatusEnum.UPLOADING.getCode().equals(record.getStatus())) {
            throw new MediaException(MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS);
        }

        try {
            // 3. 调用 S3 的取消接口
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(record.getBucket())
                .key(record.getFilePath())
                .uploadId(record.getUploadId())
                .build());

            // 4. 更新记录状态
            record.setStatus(UploadStatusEnum.ABORTED.getCode());
            record.setAbortTime(new Date());
            multipartUploadRecordRepository.save(record);
        } catch (Exception e) {
            log.error("取消分片上传失败", e);
            throw new MediaException(MediaErrorCode.ABORT_MULTIPART_UPLOAD_FAILED);
        }
    }

    @Override
    public UploadStatusResponseDTO getUploadStatus(String uploadId) {
        // 1. 获取上传记录
        MultipartUploadRecord record = multipartUploadRecordRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new MediaException(MediaErrorCode.UPLOAD_SESSION_NOT_FOUND));

        // 2. 获取已上传分片信息
        List<UploadedPartDTO> uploadedParts = new ArrayList<>();
        try {
            ListPartsResponse listPartsResponse = s3Client.listParts(ListPartsRequest.builder()
                .bucket(record.getBucket())
                .key(record.getFilePath())
                .uploadId(record.getUploadId())
                .build());

            // 转换 S3 的 Part 对象到我们的 DTO
            uploadedParts = listPartsResponse.parts().stream()
                .map(part -> UploadedPartDTO.builder()
                    .partNumber(part.partNumber())
                    .eTag(part.eTag())
                    .size(part.size())
                    .build())
                .collect(Collectors.toList());
        } catch (NoSuchUploadException e) {
            // 如果上传已经被取消或完成，S3 会抛出这个异常
            log.warn("上传已不存在: uploadId={}", uploadId);
        } catch (Exception e) {
            log.error("获取分片信息失败: uploadId={}", uploadId, e);
            throw new MediaException(MediaErrorCode.GET_UPLOAD_STATUS_FAILED);
        }

        // 3. 构建响应
        return UploadStatusResponseDTO.builder()
            .uploadId(record.getUploadId())
            .status(record.getStatus())
            .totalChunks(record.getTotalChunks())
            .uploadedParts(uploadedParts)
            .expirationTime(record.getExpirationTime().getTime())
            .build();
    }
} 