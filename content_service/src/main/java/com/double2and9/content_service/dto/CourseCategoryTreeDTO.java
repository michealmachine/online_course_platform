package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import java.util.List;

@Data
@ToString
public class CourseCategoryTreeDTO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    // 子节点
    private List<CourseCategoryTreeDTO> childrenTreeNodes;
} 