package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.CourseBaseDTO;
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
     * 查询机构教师列表
     * @param organizationId 机构ID
     * @return 教师列表
     */
    List<CourseTeacherDTO> listByOrganizationId(Long organizationId);

    /**
     * 根据教师ID查询其关联的所有课程
     * @param teacherId 教师ID
     * @return 课程列表
     */
    List<CourseBaseDTO> listCoursesByTeacherId(Long teacherId);

    /**
     * 根据机构ID和教师ID查询教师信息
     * @param organizationId 机构ID
     * @param teacherId 教师ID
     * @return 教师信息
     */
    CourseTeacherDTO getTeacherDetail(Long organizationId, Long teacherId);

    /**
     * 保存课程教师信息
     * @param teacherDTO 教师信息
     */
    void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO);

    /**
     * 解除教师与课程的关联
     * @param courseId 课程ID
     * @param teacherId 教师ID
     */
    void deleteCourseTeacher(Long courseId, Long teacherId);
} 