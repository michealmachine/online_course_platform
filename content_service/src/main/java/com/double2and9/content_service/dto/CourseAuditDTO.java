package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "课程审核信息")
public class CourseAuditDTO {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "审核状态")
    private String auditStatus;

    @Schema(description = "审核意见")
    private String auditMessage;

    @Schema(description = "审核人ID", hidden = true) // hidden=true 表示不在swagger文档中显示
    private Long auditorId; // 由Controller注入,不需要前端传入

    public static final String AUDIT_STATUS_PASS = "pass"; // 审核通过
    public static final String AUDIT_STATUS_REJECT = "reject"; // 审核不通过
}