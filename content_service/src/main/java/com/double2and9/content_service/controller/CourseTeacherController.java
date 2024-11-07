package com.double2and9.content_service.controller;

import com.double2and9.content_service.dto.CourseTeacherDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import com.double2and9.content_service.service.CourseTeacherService;
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
    @GetMapping("/list/{courseId}")
    public List<CourseTeacherDTO> listByCourseId(@PathVariable Long courseId) {
        return courseTeacherService.listByCourseId(courseId);
    }

    /**
     * 添加或修改课程教师
     */
    @PostMapping
    public void saveCourseTeacher(@RequestBody @Validated SaveCourseTeacherDTO teacherDTO) {
        courseTeacherService.saveCourseTeacher(teacherDTO);
    }

    /**
     * 删除课程教师
     */
    @DeleteMapping("/{courseId}/{teacherId}")
    public void deleteCourseTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        courseTeacherService.deleteCourseTeacher(courseId, teacherId);
    }
} 