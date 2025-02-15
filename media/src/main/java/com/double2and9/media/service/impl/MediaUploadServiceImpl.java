package com.double2and9.media.service.impl;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.InitiateMultipartUploadRequestDTO;
import com.double2and9.media.dto.InitiateMultipartUploadResponseDTO;
import com.double2and9.media.dto.GetPresignedUrlRequestDTO;
import com.double2and9.media.dto.GetPresignedUrlResponseDTO;
import com.double2and9.media.entity.MultipartUploadRecord;
import com.double2and9.media.repository.MultipartUploadRecordRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadServiceImpl implements MediaUploadService {

    private final MultipartUploadRecordRepository multipartUploadRecordRepository;
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String defaultBucketName;
    
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
        record.setFileName(request.getFileName());
        record.setFileSize(request.getFileSize());
        record.setBucket(defaultBucketName);
        record.setFilePath(filePath);
        record.setTotalChunks(totalChunks);
        record.setUploadedChunks(0);
        record.setChunkSize(chunkSize);
        record.setStatus("UPLOADING");
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
} 