package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.CourseTeacherDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;

import java.util.List;

public interface CourseTeacherService {
    /**
     * 查询课程教师列表
     * @param courseId 课程ID
     * @return 教师列表
     */
    List<CourseTeacherDTO> listByCourseId(Long courseId);

    /**
     * 保存课程教师信息
     * @param teacherDTO 教师信息
     */
    void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO);

    /**
     * 删除课程教师
     * @param courseId 课程ID
     * @param teacherId 教师ID
     */
    void deleteCourseTeacher(Long courseId, Long teacherId);
} 