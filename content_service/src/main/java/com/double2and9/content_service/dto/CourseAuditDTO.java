package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "课程审核信息")
public class CourseAuditDTO {
    
    @NotNull(message = "课程ID不能为空")
    @Schema(description = "课程ID", required = true)
    private Long courseId;
    
    @NotNull(message = "审核状态不能为空")
    @Schema(description = "审核状态", required = true)
    private String auditStatus;
    
    @Schema(description = "审核意见")
    private String auditMessage;

}