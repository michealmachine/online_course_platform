package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import java.util.List;

@Data
@ToString
public class CoursePreviewDTO {
    // 课程基本信息
    private CourseBaseDTO courseBase;
    // 课程计划信息
    private List<TeachplanDTO> teachplans;
    // 课程教师信息
    private List<CourseTeacherDTO> teachers;
} 