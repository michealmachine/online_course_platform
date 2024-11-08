package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@ToString
@Schema(description = "课程计划媒资关联DTO")
public class TeachplanMediaDTO {
    @Schema(description = "课程计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "课程计划ID不能为空")
    private Long teachplanId;
    
    @Schema(description = "媒资文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "媒资文件ID不能为空")
    private Long mediaId;
    
    @Schema(description = "媒资文件名称")
    private String mediaFileName;
    
    @Schema(description = "媒资文件类型")
    private String mediaType;
    
    @Schema(description = "媒资文件URL")
    private String url;
} 