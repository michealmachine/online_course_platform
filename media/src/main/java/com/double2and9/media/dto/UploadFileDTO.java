package com.double2and9.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Schema(description = "文件上传DTO")
public class UploadFileDTO {
    
    @Schema(description = "文件名")
    private String fileName;
    
    @Schema(description = "文件类型")
    private String fileType;
    
    @Schema(description = "文件大小")
    private Long fileSize;
    
    @Schema(description = "文件MD5值")
    private String fileMd5;
    
    @Schema(description = "文件路径")
    private String filePath;
    
    @Schema(description = "文件数据")
    private byte[] fileData;
    
    @Schema(description = "上传用户")
    private String username;
    
    @Schema(description = "文件用途")
    private String purpose;
} 