package com.double2and9.content_service.dto;

import lombok.Data;
import lombok.ToString;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;

@Data
@ToString
public class SaveCourseTeacherDTO {
    private Long id;

    @NotNull(message = "机构ID不能为空")
    private Long organizationId;

    @NotBlank(message = "教师名称不能为空")
    private String name;

    @NotBlank(message = "教师职位不能为空")
    private String position;

    private String description;
}