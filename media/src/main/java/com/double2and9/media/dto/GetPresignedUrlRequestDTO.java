package com.double2and9.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 获取预签名URL请求DTO
 */
@Data
@Schema(description = "获取预签名URL请求")
public class GetPresignedUrlRequestDTO {
    
    @Schema(description = "上传ID", required = true)
    @NotBlank(message = "上传ID不能为空")
    private String uploadId;
    
    @Schema(description = "分片索引", required = true)
    @Min(value = 1, message = "分片索引必须大于等于1")
    private Integer chunkIndex;
} 