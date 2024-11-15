package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@ToString
@Schema(description = "课程审核DTO")
public class CourseAuditDTO {
    @Schema(description = "课程ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    
    @Schema(description = "审核状态：202301-已提交，202302-审核中，202303-通过，202304-不通过", 
           example = "202303", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "审核状态不能为空")
    private String auditStatus;
    
    @Schema(description = "审核意见")
    private String auditMind;
} 