package com.double2and9.content_service.dto;

import java.math.BigDecimal;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CourseBaseDTO {
    // 课程基本信息
    private Long id;
    private String name;
    private String brief;
    private String logo;
    private String charge;
    private String status;
    private BigDecimal price;
    
    // 课程分类名称
    private String mtName;
    private String stName;
    
    // 课程营销信息
    private String discounts;
    private String teachmode;
} 