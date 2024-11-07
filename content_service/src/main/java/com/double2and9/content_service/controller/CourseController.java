package com.double2and9.content_service.controller;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.dto.QueryCourseParamsDTO;
import com.double2and9.content_service.dto.CourseCategoryTreeDTO;
import com.double2and9.content_service.dto.AddCourseDTO;
import com.double2and9.content_service.dto.EditCourseDTO;
import com.double2and9.content_service.dto.CoursePreviewDTO;
import com.double2and9.content_service.dto.CourseAuditDTO;
import com.double2and9.content_service.service.CourseBaseService;
import com.double2and9.content_service.common.model.ContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/course")
public class CourseController {

    private final CourseBaseService courseBaseService;

    public CourseController(CourseBaseService courseBaseService) {
        this.courseBaseService = courseBaseService;
    }

    /**
     * 课程列表分页查询
     * @param pageParams 分页参数
     * @param queryParams 查询条件
     * @return 分页结果
     */
    @GetMapping("/list")
    public ContentResponse<PageResult<CourseBaseDTO>> list(PageParams pageParams, QueryCourseParamsDTO queryParams) {
        log.info("课程查询，分页参数：{}，查询参数：{}", pageParams, queryParams);
        PageResult<CourseBaseDTO> result = courseBaseService.queryCourseList(pageParams, queryParams);
        return ContentResponse.success(result);
    }

    /**
     * 根据课程状态查询课程列表
     * @param status 课程状态
     * @param pageParams 分页参数
     * @return 分页结果
     */
    @GetMapping("/list/status/{status}")
    public PageResult<CourseBaseDTO> listByStatus(
            @PathVariable String status,
            PageParams pageParams) {
        QueryCourseParamsDTO queryParams = new QueryCourseParamsDTO();
        queryParams.setStatus(status);
        return courseBaseService.queryCourseList(pageParams, queryParams);
    }

    /**
     * 根据课程分类查询课程列表
     * @param mt 课程大分类
     * @param st 课程小分类
     * @param pageParams 分页参数
     * @return 分页结果
     */
    @GetMapping("/list/category")
    public PageResult<CourseBaseDTO> listByCategory(
            @RequestParam(required = false) Long mt,
            @RequestParam(required = false) Long st,
            PageParams pageParams) {
        QueryCourseParamsDTO queryParams = new QueryCourseParamsDTO();
        queryParams.setMt(mt);
        queryParams.setSt(st);
        return courseBaseService.queryCourseList(pageParams, queryParams);
    }

    /**
     * 查询课程分类树
     * @return 课程分类树
     */
    @GetMapping("/category/tree")
    public List<CourseCategoryTreeDTO> categoryTree() {
        return courseBaseService.queryCourseCategoryTree();
    }

    /**
     * 创建课程
     */
    @PostMapping
    public Long createCourse(@RequestBody @Validated AddCourseDTO addCourseDTO) {
        return courseBaseService.createCourse(addCourseDTO);
    }

    /**
     * 修改课程
     */
    @PutMapping
    public void updateCourse(@RequestBody @Validated EditCourseDTO editCourseDTO) {
        courseBaseService.updateCourse(editCourseDTO);
    }

    /**
     * 课程预览
     */
    @GetMapping("/preview/{courseId}")
    public CoursePreviewDTO previewCourse(@PathVariable Long courseId) {
        return courseBaseService.preview(courseId);
    }

    /**
     * 提交课程审核
     */
    @PostMapping("/{courseId}/audit/submit")
    public void submitForAudit(@PathVariable Long courseId) {
        courseBaseService.submitForAudit(courseId);
    }

    /**
     * 审核课程
     */
    @PostMapping("/audit")
    public void auditCourse(@RequestBody @Validated CourseAuditDTO auditDTO) {
        courseBaseService.auditCourse(auditDTO);
    }

    /**
     * 获取课程审核状态
     */
    @GetMapping("/{courseId}/audit/status")
    public String getAuditStatus(@PathVariable Long courseId) {
        return courseBaseService.getAuditStatus(courseId);
    }
} 