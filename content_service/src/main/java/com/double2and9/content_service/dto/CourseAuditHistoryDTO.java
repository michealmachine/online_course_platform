package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;

@Data
@Schema(description = "课程审核历史")
public class CourseAuditHistoryDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "审核人名称")
    private String auditorName;

    @Schema(description = "审核状态")
    private String auditStatus;

    @Schema(description = "审核意见")
    private String auditMessage;

    @Schema(description = "审核时间")
    private Date auditTime;
}