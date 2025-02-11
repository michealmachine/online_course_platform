package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;

@Data
@Schema(description = "课程预发布信息")
public class CoursePublishPreDTO {

    @Schema(description = "课程ID")
    private Long id;

    @Schema(description = "课程名称")
    private String name;

    @Schema(description = "审核状态")
    private String status;

    @Schema(description = "审核意见")
    private String auditMessage;

    @Schema(description = "预览时间")
    private Date previewTime;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;
}