package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@ToString
public class CourseAuditDTO {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    
    @NotEmpty(message = "审核状态不能为空")
    private String auditStatus;  // 202301-已提交，202302-审核中，202303-通过，202304-不通过
    
    private String auditMind;    // 审核意见
} 