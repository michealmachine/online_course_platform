package com.double2and9.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Schema(description = "媒资文件DTO")
public class MediaFileDTO {
    @Schema(description = "媒资文件ID")
    private String mediaFileId;  // media服务的文件ID
    
    @Schema(description = "文件名称")
    private String fileName;
    
    @Schema(description = "文件类型")
    private String mediaType;    // IMAGE/VIDEO
    
    @Schema(description = "文件大小")
    private Long fileSize;
    
    @Schema(description = "文件MIME类型")
    private String mimeType;
    
    @Schema(description = "文件用途")
    private String purpose;      // COVER/VIDEO
    
    @Schema(description = "文件访问地址")
    private String url;         // 图片直接使用，视频需要请求临时地址
    
    // organizationId不需要在DTO中，因为会从Security上下文获取
} 