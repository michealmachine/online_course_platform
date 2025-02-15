package com.double2and9.media.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 初始化分片上传请求DTO
 */
@Data
public class InitiateMultipartUploadRequestDTO {
    
    @NotBlank(message = "文件名不能为空")
    private String fileName;
    
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;
    
    @NotBlank(message = "媒体类型不能为空")
    private String mediaType;
    
    private String mimeType;
    
    private String purpose;
    
    @NotNull(message = "机构ID不能为空")
    private Long organizationId;
} 