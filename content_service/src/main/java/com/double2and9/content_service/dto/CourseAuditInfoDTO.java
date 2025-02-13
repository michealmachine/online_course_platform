package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "课程审核信息DTO")
public class CourseAuditInfoDTO {
    
    @Schema(description = "课程基本信息")
    private CourseBaseDTO courseBase;
    
    @Schema(description = "审核状态")
    private String auditStatus;
    
    @Schema(description = "审核意见")
    private String auditMessage;
    
    @Schema(description = "最近审核时间")
    private LocalDateTime lastAuditTime;
} 