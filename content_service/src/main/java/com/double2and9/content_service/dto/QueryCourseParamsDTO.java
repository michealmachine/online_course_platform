package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryCourseParamsDTO {
    //课程名称
    private String courseName;
    //课程状态
    private String status;
    //课程审核状态
    private String auditStatus;
    //课程大分类
    private Long mt;
    //课程小分类
    private Long st;
} 