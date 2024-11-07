package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@ToString
public class SaveCourseTeacherDTO {
    private Long id;
    
    @NotNull(message = "课程id不能为空")
    private Long courseId;
    
    @NotEmpty(message = "教师名称不能为空")
    private String name;
    
    private String position;
    private String description;
} 