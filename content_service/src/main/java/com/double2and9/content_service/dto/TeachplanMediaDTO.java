package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotNull;

@Data
@ToString
public class TeachplanMediaDTO {
    @NotNull(message = "课程计划ID不能为空")
    private Long teachplanId;
    
    @NotNull(message = "媒资文件ID不能为空")
    private Long mediaId;
    
    private String mediaFileName;
    private String mediaType;
    private String url;
} 