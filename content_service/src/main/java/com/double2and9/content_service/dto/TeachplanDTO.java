package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import java.util.List;

@Data
@ToString
public class TeachplanDTO {
    private Long id;
    private String name;
    private Long courseId;
    private Long parentId;
    private Integer level;
    private Integer orderBy;
    // 子节点
    private List<TeachplanDTO> teachPlanTreeNodes;
} 