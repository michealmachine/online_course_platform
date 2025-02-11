package com.double2and9.content_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@ToString
@Schema(description = "课程基本信息DTO")
public class CourseBaseDTO {
    @Schema(description = "课程ID")
    private Long id;

    @Schema(description = "课程名称")
    private String name;

    @Schema(description = "课程简介")
    private String brief;

    @Schema(description = "课程封面图片URL，为空时使用默认图片")
    private String logo;

    @Schema(description = "收费规则")
    private String charge;

    @Schema(description = "课程状态")
    private String status;

    @Schema(description = "课程价格")
    private BigDecimal price;

    @Schema(description = "课程大分类名称")
    private String mtName;

    @Schema(description = "课程小分类名称")
    private String stName;

    @Schema(description = "优惠信息")
    private String discounts;

    @Schema(description = "教学模式")
    private String teachmode;

    @Schema(description = "机构ID")
    private Long organizationId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}