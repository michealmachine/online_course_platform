package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "课程发布信息DTO")
public class CoursePublishDTO {
    @Schema(description = "课程ID")
    private Long id;

    @Schema(description = "课程名称")
    private String name;

    @Schema(description = "发布状态")
    private String status;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
} 