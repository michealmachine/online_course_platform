package com.double2and9.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 完成分片上传响应DTO
 */
@Data
@Builder
@Schema(description = "完成分片上传响应数据")
public class CompleteMultipartUploadResponseDTO {
    
    @Schema(description = "媒体文件ID", example = "org_1_abc123")
    private String mediaFileId;
    
    @Schema(description = "文件访问URL", 
            example = "https://media.example.com/video/1/org_1_abc123.mp4")
    private String fileUrl;
    
    @Schema(description = "文件大小（字节）", example = "15728640")
    private Long fileSize;
    
    @Schema(description = "文件状态", example = "COMPLETED")
    private String status;
    
    @Schema(description = "完成时间（毫秒时间戳）", example = "1677600000000")
    private Long completeTime;
} 