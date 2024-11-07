package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CourseTeacherDTO {
    private Long id;
    private Long courseId;
    private String name;
    private String position;
    private String description;
} 