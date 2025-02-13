package com.double2and9.content_service.service;

import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.dto.*;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 查询已审核通过的课程列表
     * 用于普通用户浏览课程
     * 
     * @param params      分页参数
     * @param queryParams 查询条件(只支持课程名称搜索)
     * @return 分页结果
     */
    PageResult<CourseBaseDTO> queryApprovedCourseList(PageParams params, QueryCourseParamsDTO queryParams);

    /**
     * 查询机构的课程列表
     * 用于机构管理自己的课程
     * 
     * @param params      分页参数
     * @param queryParams 查询条件(支持课程名称、状态等搜索)
     * @return 分页结果
     */
    PageResult<CourseBaseDTO> queryCourseList(PageParams params, QueryCourseParamsDTO queryParams);

    /**
     * 查询机构的课程列表
     * 用于机构管理自己的课程
     * 
     * @param orgId  机构ID
     * @param params 分页参数
     **/
    PageResult<CourseBaseDTO> queryCourseListByOrg(Long orgId, PageParams params, QueryCourseParamsDTO queryParams);

    /**
     * 查询课程分类树
     * 
     * @return 课程分类树
     */
    List<CourseCategoryTreeDTO> queryCourseCategoryTree();

    @Transactional
    void updateCourse(EditCourseDTO editCourseDTO);

    /**
     * 获取课程审核状态
     * 
     * @param courseId 课程ID
     * @param organizationId 机构ID
     * @return 审核状态
     * @throws ContentException 当课程不存在或无权限访问时
     */
    String getAuditStatus(Long courseId, Long organizationId);

    /**
     * 根据ID获取课程
     * 
     * @param courseId 课程ID
     * @return 课程基本信息
     */
    CourseBaseDTO getCourseById(Long courseId);

    /**
     * 删除课程
     * 
     * @param courseId 课程ID
     */
    void deleteCourse(Long courseId);

    /**
     * 下架课程
     * 
     * @param courseId 课程ID
     */
    void offlineCourse(Long courseId);

    /**
     * 上传课程封面到临时存储
     * 
     * @param courseId 课程ID
     * @param file     封面图片文件
     * @return 临时存储的key
     */
    String uploadCourseLogoTemp(Long courseId, MultipartFile file);

    /**
     * 确认并保存临时课程封面
     * 
     * @param courseId 课程ID
     * @param tempKey  临时存储key
     */
    void confirmCourseLogo(Long courseId, String tempKey);

    /**
     * 删除课程封面
     * 
     * @param courseId 课程ID
     */
    void deleteCourseLogo(Long courseId);

    void deleteCourseWithRelations(Long courseId, boolean force);
}