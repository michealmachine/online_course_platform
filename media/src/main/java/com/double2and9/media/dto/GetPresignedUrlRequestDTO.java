package com.double2and9.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 获取预签名URL请求DTO
 */
@Data
@Schema(description = "获取预签名URL请求参数")
public class GetPresignedUrlRequestDTO {
    
    @Schema(description = "分片上传ID", example = "upload-123")
    @NotBlank(message = "上传ID不能为空")
    private String uploadId;
    
    @Schema(description = "分片索引，从1开始", example = "1")
    @NotNull(message = "分片索引不能为空")
    @Positive(message = "分片索引必须大于0")
    private Integer chunkIndex;
} 