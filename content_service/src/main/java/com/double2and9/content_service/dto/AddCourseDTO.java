package com.double2and9.content_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@ToString
@Schema(description = "课程创建DTO")
public class AddCourseDTO {
    // 课程基本信息
    @Schema(description = "课程名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "课程名称不能为空")
    private String name;
    
    @Schema(description = "课程简介")
    @NotEmpty(message = "课程简介不能为空")
    private String brief;
    
    @Schema(description = "课程封面图片URL，非必填", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String logo;
    
    @Schema(description = "课程大分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "课程大分类不能为空")
    private Long mt;
    
    @Schema(description = "课程小分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "课程小分类不能为空")
    private Long st;
    
    // 课程营销信息
    @Schema(description = "收费规则，例如：201001-免费，201002-收费", example = "201001")
    @NotEmpty(message = "收费规则不能为空")
    private String charge;
    
    @Schema(description = "课程价格", example = "0.00")
    private BigDecimal price;
    
    @Schema(description = "原价", example = "100.00")
    private BigDecimal priceOld;
    
    @Schema(description = "QQ", example = "12345678")
    private String qq;
    
    @Schema(description = "优惠信息")
    private String discounts;
    
    @Schema(description = "是否有效", defaultValue = "true")
    private Boolean valid;
    
    @Schema(description = "机构ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "机构ID不能为空")
    private Long organizationId;
} 