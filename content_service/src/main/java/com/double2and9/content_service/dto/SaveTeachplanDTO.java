package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@ToString
public class SaveTeachplanDTO {
    private Long id;
    
    @NotNull(message = "课程id不能为空")
    private Long courseId;
    
    @NotEmpty(message = "课程计划名称不能为空")
    private String name;
    
    @NotNull(message = "父级课程计划id不能为空")
    private Long parentId;
    
    @NotNull(message = "层级不能为空")
    private Integer level;
    
    private Integer orderBy;
} 