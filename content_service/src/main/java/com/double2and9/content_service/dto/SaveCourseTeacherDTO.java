package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

@Data
@ToString
public class SaveCourseTeacherDTO {
    private Long id;
    
    @NotNull(message = "机构ID不能为空")
    private Long organizationId;
    
    @NotEmpty(message = "教师名称不能为空")
    private String name;
    
    private String position;
    
    private String description;
    
    @NotEmpty(message = "课程ID列表不能为空")
    private Set<Long> courseIds;
} 