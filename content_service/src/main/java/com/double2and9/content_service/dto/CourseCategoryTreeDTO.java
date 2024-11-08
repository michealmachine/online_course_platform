package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@ToString
@Schema(description = "课程分类树形结构DTO")
public class CourseCategoryTreeDTO {
    @Schema(description = "分类ID")
    private Long id;
    
    @Schema(description = "分类名称")
    private String name;
    
    @Schema(description = "父级分类ID")
    private Long parentId;
    
    @Schema(description = "层级")
    private Integer level;
    
    @Schema(description = "子节点列表")
    private List<CourseCategoryTreeDTO> childrenTreeNodes;
} 