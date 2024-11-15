package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import java.util.List;

@Data
@ToString
@Schema(description = "课程预览信息DTO")
public class CoursePreviewDTO {
    @Schema(description = "课程基本信息")
    private CourseBaseDTO courseBase;
    
    @Schema(description = "课程计划列表")
    private List<TeachplanDTO> teachplans;
    
    @Schema(description = "课程教师列表")
    private List<CourseTeacherDTO> teachers;
} 