package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@ToString
@Schema(description = "课程计划保存DTO")
public class SaveTeachplanDTO {
    @Schema(description = "课程计划ID，新增时为空")
    private Long id;
    
    @Schema(description = "课程ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    
    @Schema(description = "课程计划名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "课程计划名称不能为空")
    private String name;
    
    @Schema(description = "父级ID，章节为0", example = "0")
    @NotNull(message = "父级ID不能为空")
    private Long parentId;
    
    @Schema(description = "层级，1:章节，2:小节", example = "1")
    @NotNull(message = "层级不能为空")
    private Integer level;
    
    @Schema(description = "排序号")
    private Integer orderBy;
} 