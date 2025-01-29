package com.double2and9.content_service.service;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.dto.*;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

public interface CourseBaseService {
    /**
     * 创建课程基本信息和营销信息
     */
    Long createCourse(AddCourseDTO addCourseDTO);
    
    /**
     * 保存课程计划
     */
    void saveTeachplan(SaveTeachplanDTO teachplanDTO);
    
    /**
     * 保存课程教师信息
     */
    void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO);
    
    /**
     * 课程预览
     */
    CoursePreviewDTO preview(Long courseId);

    
    /**
     * 发布课程
     */
    void publishCourse(Long courseId);

    PageResult<CourseBaseDTO> queryCourseList(PageParams params, QueryCourseParamsDTO queryParams);

    List<CourseCategoryTreeDTO> queryCourseCategoryTree();

    @Transactional
    void updateCourse(EditCourseDTO editCourseDTO);

    /**
     * 提交课程审核
     * @param courseId 课程ID
     */
    void submitForAudit(Long courseId);

    /**
     * 审核课程
     * @param auditDTO 审核信息
     */
    void auditCourse(CourseAuditDTO auditDTO);

    /**
     * 获取课程审核状态
     * @param courseId 课程ID
     * @return 审核状态
     */
    String getAuditStatus(Long courseId);

    /**
     * 根据ID获取课程
     * @param courseId 课程ID
     * @return 课程基本信息
     */
    CourseBaseDTO getCourseById(Long courseId);

    /**
     * 删除课程
     * @param courseId 课程ID
     */
    void deleteCourse(Long courseId);

    /**
     * 下架课程
     * @param courseId 课程ID
     */
    void offlineCourse(Long courseId);
}