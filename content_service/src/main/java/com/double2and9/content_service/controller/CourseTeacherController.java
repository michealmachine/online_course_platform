package com.double2and9.content_service.controller;

import com.double2and9.content_service.common.model.ContentResponse;
import com.double2and9.content_service.dto.CourseTeacherDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import com.double2and9.content_service.service.CourseTeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/course-teacher")
public class CourseTeacherController {

    private final CourseTeacherService courseTeacherService;

    public CourseTeacherController(CourseTeacherService courseTeacherService) {
        this.courseTeacherService = courseTeacherService;
    }

    /**
     * 查询课程教师列表
     */
    @Operation(summary = "查询课程教师列表")
    @GetMapping("/list/{courseId}")
    public ContentResponse<List<CourseTeacherDTO>> listByCourseId(
            @Parameter(description = "课程ID") @PathVariable Long courseId) {
        return ContentResponse.success(courseTeacherService.listByCourseId(courseId));
    }

    /**
     * 添加或修改课程教师
     */
    @Operation(summary = "添加或修改课程教师")
    @PostMapping
    public ContentResponse<Void> saveCourseTeacher(
            @Parameter(description = "教师信息") @RequestBody @Validated SaveCourseTeacherDTO teacherDTO) {
        courseTeacherService.saveCourseTeacher(teacherDTO);
        return ContentResponse.success(null);
    }

    /**
     * 删除课程教师
     */
    @Operation(summary = "删除课程教师")
    @DeleteMapping("/{courseId}/{teacherId}")
    public ContentResponse<Void> deleteCourseTeacher(
            @Parameter(description = "课程ID") @PathVariable Long courseId,
            @Parameter(description = "教师ID") @PathVariable Long teacherId) {
        courseTeacherService.deleteCourseTeacher(courseId, teacherId);
        return ContentResponse.success(null);
    }
} 