package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@ToString
@Schema(description = "课程查询参数DTO")
public class QueryCourseParamsDTO {
    @Schema(description = "课程名称")
    private String courseName;
    
    @Schema(description = "课程状态")
    private String status;
    
    @Schema(description = "课程审核状态")
    private String auditStatus;
    
    @Schema(description = "课程大分类")
    private Long mt;
    
    @Schema(description = "课程小分类")
    private Long st;
    
    @Schema(description = "机构ID")
    private Long organizationId;
} 