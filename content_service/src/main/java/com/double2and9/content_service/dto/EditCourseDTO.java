package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@ToString
public class EditCourseDTO {
    @NotNull(message = "课程id不能为空")
    private Long id;
    
    @NotEmpty(message = "课程名称不能为空")
    private String name;
    
    @NotEmpty(message = "课程简介不能为空")
    private String brief;
    
    private String logo;
    
    @NotNull(message = "课程大分类不能为空")
    private Long mt;
    
    @NotNull(message = "课程小分类不能为空")
    private Long st;
    
    @NotEmpty(message = "收费规则不能为空")
    private String charge;
    
    private BigDecimal price;
    private BigDecimal priceOld;
    private String qq;
    private String discounts;
    private Boolean valid;
} 