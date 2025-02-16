package com.double2and9.media.service.impl;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.base.enums.UploadStatusEnum;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.*;
import com.double2and9.media.entity.MultipartUploadRecord;
import com.double2and9.media.repository.MultipartUploadRecordRepository;
import com.double2and9.media.repository.MediaFileRepository;
import com.double2and9.media.service.MediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import com.double2and9.base.enums.MediaFileStatusEnum;
import com.double2and9.media.entity.MediaFile;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadServiceImpl implements MediaUploadService {

    private final MultipartUploadRecordRepository multipartUploadRecordRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MinioClient minioClient;
    
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
        String uploadId = UUID.randomUUID().toString();
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
        
        // 4. 创建上传记录
        MultipartUploadRecord record = new MultipartUploadRecord();
        record.setUploadId(uploadId);
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
        record.setUploadedChunks(0);
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
        log.debug("查询上传记录: uploadId={}", request.getUploadId());
        MultipartUploadRecord record = multipartUploadRecordRepository.findByUploadId(request.getUploadId())
                .orElseThrow(() -> new MediaException(MediaErrorCode.UPLOAD_SESSION_NOT_FOUND));
        log.debug("查询到上传记录: record={}", record);
        
        // 2. 验证上传状态
        if (!"UPLOADING".equals(record.getStatus())) {
            log.warn("上传会话状态无效: uploadId={}, status={}", request.getUploadId(), record.getStatus());
            throw new MediaException(MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS);
        }
        
        // 3. 验证分片索引
        if (request.getChunkIndex() > record.getTotalChunks()) {
            log.warn("分片索引无效: uploadId={}, chunkIndex={}, totalChunks={}", 
                    request.getUploadId(), 
                    request.getChunkIndex(), 
                    record.getTotalChunks());
            throw new MediaException(MediaErrorCode.INVALID_CHUNK_INDEX);
        }
        
        try {
            // 4. 生成分片对象名称
            String chunkObjectName = String.format("%s.part%d", 
                    record.getFilePath(), 
                    request.getChunkIndex());
            log.debug("生成分片对象名称: chunkObjectName={}", chunkObjectName);
            
            // 5. 生成预签名URL
            log.debug("开始生成预签名URL: bucket={}, object={}, expiry={}s",
                    record.getBucket(),
                    chunkObjectName,
                    presignedUrlExpirationSeconds);
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(record.getBucket())
                            .object(chunkObjectName)
                            .expiry(presignedUrlExpirationSeconds, TimeUnit.SECONDS)
                            .build());
            
            // 6. 计算过期时间
            long expirationTime = System.currentTimeMillis() + 
                    (presignedUrlExpirationSeconds * 1000L);
            
            // 7. 构建响应
            GetPresignedUrlResponseDTO response = GetPresignedUrlResponseDTO.builder()
                    .presignedUrl(presignedUrl)
                    .chunkIndex(request.getChunkIndex())
                    .expirationTime(expirationTime)
                    .build();
            
            log.info("成功生成预签名URL: uploadId={}, chunkIndex={}, expirationTime={}, presignedUrl={}",
                    request.getUploadId(),
                    request.getChunkIndex(),
                    expirationTime,
                    presignedUrl);
            
            return response;
            
        } catch (Exception e) {
            log.error("生成预签名URL失败: uploadId={}, chunkIndex={}, error={}", 
                    request.getUploadId(), 
                    request.getChunkIndex(), 
                    e.getMessage(),
                    e);
            throw new MediaException(MediaErrorCode.GENERATE_PRESIGNED_URL_FAILED);
        }
    }

    @Override
    @Transactional
    public CompleteMultipartUploadResponseDTO completeMultipartUpload(CompleteMultipartUploadRequestDTO request) {
        log.info("开始处理完成分片上传请求: request={}", request);
        
        try {
            // 1. 查询上传记录
            MultipartUploadRecord record = multipartUploadRecordRepository.findByUploadId(request.getUploadId())
                    .orElseThrow(() -> new MediaException(MediaErrorCode.UPLOAD_SESSION_NOT_FOUND));
            
            // 2. 验证上传状态
            if (!UploadStatusEnum.UPLOADING.getCode().equals(record.getStatus())) {
                log.warn("上传会话状态无效: uploadId={}, status={}", 
                        request.getUploadId(), record.getStatus());
                throw new MediaException(MediaErrorCode.UPLOAD_SESSION_INVALID_STATUS);
            }
            
            // 3. 验证分片数量
            if (record.getUploadedChunks() < record.getTotalChunks()) {
                log.warn("分片未上传完成: uploadId={}, uploadedChunks={}, totalChunks={}", 
                        request.getUploadId(), 
                        record.getUploadedChunks(), 
                        record.getTotalChunks());
                throw new MediaException(MediaErrorCode.UPLOAD_NOT_COMPLETED);
            }
            
            // 4. 分片合并
            log.debug("开始分片合并: uploadId={}, bucket={}, filePath={}", 
                    request.getUploadId(),
                    record.getBucket(),
                    record.getFilePath());
            
            List<ComposeSource> sources = new ArrayList<>();
            for (int i = 1; i <= record.getTotalChunks(); i++) {
                String chunkObjectName = String.format("%s.part%d", record.getFilePath(), i);
                sources.add(
                    ComposeSource.builder()
                        .bucket(record.getBucket())
                        .object(chunkObjectName)
                        .build()
                );
            }
            
            try {
                minioClient.composeObject(
                    ComposeObjectArgs.builder()
                        .bucket(record.getBucket())
                        .object(record.getFilePath())
                        .sources(sources)
                        .build()
                );
                log.debug("分片合并成功: uploadId={}", request.getUploadId());
            } catch (Exception e) {
                log.error("分片合并失败: uploadId={}, error={}", request.getUploadId(), e.getMessage(), e);
                throw new MediaException(MediaErrorCode.MERGE_CHUNKS_FAILED, e);
            }

            // 5. 更新上传记录状态
            record.setStatus(UploadStatusEnum.COMPLETED.getCode());
            record.setCompleteTime(new Date());
            record.setUploadedChunks(record.getTotalChunks());
            multipartUploadRecordRepository.save(record);
            
            // 6. 创建MediaFile记录
            log.debug("创建MediaFile记录: uploadId={}, mediaFileId={}", 
                    request.getUploadId(),
                    record.getMediaFileId());
            
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
                StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                        .bucket(record.getBucket())
                        .object(record.getFilePath())
                        .build()
                );
                mediaFile.setFileSize(stat.size());
            } catch (Exception e) {
                log.error("获取文件大小失败: uploadId={}, error={}", request.getUploadId(), e.getMessage(), e);
                mediaFile.setFileSize(record.getFileSize()); // 使用上传时提供的文件大小
            }
            
            try {
                mediaFileRepository.save(mediaFile);
                log.debug("MediaFile记录创建成功: mediaFileId={}", mediaFile.getMediaFileId());
            } catch (Exception e) {
                log.error("创建MediaFile记录失败: mediaFileId={}, error={}", 
                    mediaFile.getMediaFileId(), e.getMessage(), e);
                // 回滚状态
                record.setStatus(UploadStatusEnum.UPLOADING.getCode());
                record.setCompleteTime(null);
                multipartUploadRecordRepository.save(record);
                throw new MediaException(MediaErrorCode.CREATE_MEDIA_FILE_FAILED, e);
            }

            // 7. 构建文件访问URL
            String fileUrl = String.format("%s/%s/%s", 
                    minioEndpoint,
                    record.getBucket(), 
                    record.getFilePath());
            
            // 8. 构建响应
            CompleteMultipartUploadResponseDTO response = CompleteMultipartUploadResponseDTO.builder()
                    .mediaFileId(record.getMediaFileId())
                    .fileUrl(fileUrl)
                    .fileSize(record.getFileSize())
                    .status(UploadStatusEnum.COMPLETED.getCode())
                    .completeTime(record.getCompleteTime().getTime())
                    .build();
            
            log.info("完成分片上传成功: uploadId={}, mediaFileId={}, fileUrl={}", 
                    request.getUploadId(),
                    response.getMediaFileId(),
                    response.getFileUrl());
            
            return response;
            
        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            log.error("完成分片上传失败: uploadId={}, error={}", 
                    request.getUploadId(), 
                    e.getMessage(),
                    e);
            throw new MediaException(MediaErrorCode.COMPLETE_MULTIPART_UPLOAD_FAILED);
        }
    }
} 