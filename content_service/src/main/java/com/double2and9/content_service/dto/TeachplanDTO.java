package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import java.util.List;

@Data
@ToString
@Schema(description = "课程计划DTO")
public class TeachplanDTO {
    @Schema(description = "计划ID")
    private Long id;
    
    @Schema(description = "计划名称")
    private String name;
    
    @Schema(description = "课程ID")
    private Long courseId;
    
    @Schema(description = "父级ID")
    private Long parentId;
    
    @Schema(description = "层级，1:章节，2:小节")
    private Integer level;
    
    @Schema(description = "排序号")
    private Integer orderBy;
    
    @Schema(description = "子节点列表")
    private List<TeachplanDTO> teachPlanTreeNodes;
} 