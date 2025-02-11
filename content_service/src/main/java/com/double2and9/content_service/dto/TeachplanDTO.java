package com.double2and9.content_service.dto;

import com.double2and9.content_service.dto.base.TreeNodeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}