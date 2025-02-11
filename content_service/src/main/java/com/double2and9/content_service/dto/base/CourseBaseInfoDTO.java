package com.double2and9.content_service.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "课程基础信息DTO")
public class CourseBaseInfoDTO {

    @Schema(description = "课程ID")
    private Long id;

    @Schema(description = "课程名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "课程名称不能为空")
    private String name;

    @Schema(description = "课程简介", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "课程简介不能为空")
    private String brief;

    @Schema(description = "课程封面图片URL")
    private String logo;

    @Schema(description = "课程大分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "课程大分类不能为空")
    private Long mt;

    @Schema(description = "课程小分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "课程小分类不能为空")
    private Long st;

    @Schema(description = "收费规则，对应数据字典", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "收费规则不能为空")
    private String charge;

    @Schema(description = "课程价格")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal priceOld;

    @Schema(description = "课程状态")
    private String status;

    @Schema(description = "机构ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "机构ID不能为空")
    private Long organizationId;
}