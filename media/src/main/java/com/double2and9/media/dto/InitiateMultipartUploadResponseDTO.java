package com.double2and9.media.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 初始化分片上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateMultipartUploadResponseDTO {
    
    /**
     * 分片上传ID
     */
    private String uploadId;
    
    /**
     * 预分配的媒体文件ID
     */
    private String mediaFileId;
    
    /**
     * 存储桶名称
     */
    private String bucket;
    
    /**
     * 文件存储路径
     */
    private String filePath;
    
    /**
     * 建议的分片大小（字节）
     */
    private Integer chunkSize;
    
    /**
     * 建议的分片数量
     */
    private Integer totalChunks;
    
    /**
     * 添加这个字段
     */
    private String s3UploadId;
} 