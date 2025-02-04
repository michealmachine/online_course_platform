package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Data
@ToString
@Schema(description = "课程教师DTO")
public class CourseTeacherDTO {
    @Schema(description = "教师ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long organizationId;

    @Schema(description = "教师名称")
    private String name;

    @Schema(description = "教师职位")
    private String position;

    @Schema(description = "教师简介")
    private String description;

    @Schema(description = "关联的课程ID列表")
    private Set<Long> courseIds;

    @Schema(description = "教师头像URL")
    private String avatar;
}