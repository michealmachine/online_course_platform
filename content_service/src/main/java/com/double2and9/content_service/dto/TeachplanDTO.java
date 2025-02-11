package com.double2and9.content_service.dto;

import com.double2and9.content_service.dto.base.TreeNodeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "课程计划DTO")
public class TeachplanDTO extends TreeNodeDTO<TeachplanDTO> {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "层级，1:章节，2:小节")
    private Integer level;

    @Schema(description = "排序号")
    private Integer orderBy;
}