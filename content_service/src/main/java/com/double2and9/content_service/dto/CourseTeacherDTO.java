package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@ToString
@Schema(description = "课程教师DTO")
public class CourseTeacherDTO {
    @Schema(description = "教师ID")
    private Long id;
    
    @Schema(description = "课程ID")
    private Long courseId;
    
    @Schema(description = "教师名称")
    private String name;
    
    @Schema(description = "教师职位")
    private String position;
    
    @Schema(description = "教师简介")
    private String description;
} 