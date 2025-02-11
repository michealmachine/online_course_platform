package com.double2and9.content_service.dto;

import com.double2and9.content_service.dto.base.TreeNodeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "课程分类树形结构DTO")
public class CourseCategoryTreeDTO extends TreeNodeDTO<CourseCategoryTreeDTO> {

    @Schema(description = "分类层级")
    private Integer level;
}