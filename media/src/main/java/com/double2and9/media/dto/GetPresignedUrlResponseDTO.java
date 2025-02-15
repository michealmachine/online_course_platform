package com.double2and9.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 获取预签名URL响应DTO
 */
@Data
@Builder
@Schema(description = "获取预签名URL响应数据")
public class GetPresignedUrlResponseDTO {
    
    @Schema(description = "预签名URL", example = "https://minio.example.com/bucket/object?X-Amz-Algorithm=...")
    private String presignedUrl;
    
    @Schema(description = "分片索引", example = "1")
    private Integer chunkIndex;
    
    @Schema(description = "过期时间（毫秒时间戳）", example = "1677600000000")
    private Long expirationTime;
} 