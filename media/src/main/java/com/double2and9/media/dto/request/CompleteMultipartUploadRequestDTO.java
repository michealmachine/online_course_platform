package com.double2and9.media.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 完成分片上传请求DTO
 */
@Data
@Schema(description = "完成分片上传请求")
public class CompleteMultipartUploadRequestDTO {
    
    @Schema(description = "上传ID", required = true)
    @NotBlank(message = "上传ID不能为空")
    private String uploadId;
} 