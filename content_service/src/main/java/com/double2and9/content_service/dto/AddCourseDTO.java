package com.double2and9.content_service.dto;

import com.double2and9.content_service.dto.base.CourseBaseInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "新增课程DTO")
public class AddCourseDTO extends CourseBaseInfoDTO {

    @Schema(description = "QQ")
    private String qq;

    @Schema(description = "优惠信息")
    private String discounts;

    @Schema(description = "是否有效", defaultValue = "true")
    private Boolean valid;
}