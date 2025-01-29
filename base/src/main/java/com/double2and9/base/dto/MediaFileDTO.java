package com.double2and9.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "媒资文件信息")
public class MediaFileDTO {
    
    @NotBlank(message = "媒资文件ID不能为空")
    @Schema(description = "媒资文件ID", required = true)
    private String mediaFileId;
    
    @NotNull(message = "机构ID不能为空")
    @Schema(description = "机构ID", required = true)
    private Long organizationId;  // 添加机构ID字段
    
    @NotBlank(message = "文件名不能为空")
    @Schema(description = "文件名", required = true)
    private String fileName;
    
    @NotBlank(message = "媒体类型不能为空")
    @Schema(description = "媒体类型：IMAGE/VIDEO", required = true)
    private String mediaType;
    
    @NotBlank(message = "文件用途不能为空")
    @Schema(description = "文件用途：COVER(封面)/VIDEO(视频)", required = true)
    private String purpose;
    
    @Schema(description = "访问地址")
    private String url;
    
    @Schema(description = "文件大小")
    private Long fileSize;
    
    @Schema(description = "文件类型")
    private String mimeType;
} 