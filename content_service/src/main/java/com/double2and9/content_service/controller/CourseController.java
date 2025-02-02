package com.double2and9.content_service.controller;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.dto.QueryCourseParamsDTO;
import com.double2and9.content_service.dto.CourseCategoryTreeDTO;
import com.double2and9.content_service.dto.AddCourseDTO;
import com.double2and9.content_service.dto.EditCourseDTO;
import com.double2and9.content_service.dto.CoursePreviewDTO;
import com.double2and9.content_service.dto.CourseAuditDTO;
import com.double2and9.content_service.service.CourseBaseService;
import com.double2and9.content_service.common.model.ContentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Tag(name = "课程管理", description = "提供课程的增删改查接口")
@Slf4j
@RestController
@RequestMapping("/course")
public class CourseController {

    private final CourseBaseService courseBaseService;

    public CourseController(CourseBaseService courseBaseService) {
        this.courseBaseService = courseBaseService;
    }

    @Operation(summary = "分页查询课程列表")
    @GetMapping("/list")
    public ContentResponse<PageResult<CourseBaseDTO>> list(
            @Parameter(description = "分页参数") PageParams pageParams,
            @Parameter(description = "查询条件") QueryCourseParamsDTO queryParams) {
        return ContentResponse.success(courseBaseService.queryCourseList(pageParams, queryParams));
    }

    @Operation(summary = "创建课程", description = "创建新的课程，包含基本信息和营销信息")
    @PostMapping
    public ContentResponse<Long> createCourse(
            @Parameter(description = "课程信息") @RequestBody @Validated AddCourseDTO addCourseDTO) {
        return ContentResponse.success(courseBaseService.createCourse(addCourseDTO));
    }

    @Operation(summary = "修改课程信息")
    @PutMapping
    public ContentResponse<Void> updateCourse(
            @Parameter(description = "课程更新信息") @RequestBody @Validated EditCourseDTO editCourseDTO) {
        courseBaseService.updateCourse(editCourseDTO);
        return ContentResponse.success(null);
    }

    @Operation(summary = "获取课程分类树")
    @GetMapping("/category/tree")
    public ContentResponse<List<CourseCategoryTreeDTO>> categoryTree() {
        return ContentResponse.success(courseBaseService.queryCourseCategoryTree());
    }

    @Operation(summary = "课程预览", description = "获取课程详细信息，包括基本信息、课程计划和教师信息")
    @GetMapping("/preview/{courseId}")
    public ContentResponse<CoursePreviewDTO> previewCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        return ContentResponse.success(courseBaseService.preview(courseId));
    }

    @Operation(summary = "提交课程审核")
    @PostMapping("/{courseId}/audit/submit")
    public ContentResponse<Void> submitForAudit(
            @Parameter(description = "课程ID") @PathVariable Long courseId) {
        courseBaseService.submitForAudit(courseId);
        return ContentResponse.success(null);
    }

    @Operation(summary = "审核课程")
    @PostMapping("/audit")
    public ContentResponse<Void> auditCourse(
            @Parameter(description = "审核信息") @RequestBody @Validated CourseAuditDTO auditDTO) {
        courseBaseService.auditCourse(auditDTO);
        return ContentResponse.success(null);
    }

    @Operation(summary = "获取课程详情")
    @GetMapping("/{courseId}")
    public ContentResponse<CourseBaseDTO> getCourseById(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("获取课程详情，courseId：{}", courseId);
        CourseBaseDTO courseBaseDTO = courseBaseService.getCourseById(courseId);
        log.info("获取课程详情成功，courseId：{}", courseId);
        return ContentResponse.success(courseBaseDTO);
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{courseId}")
    public ContentResponse<Void> deleteCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("删除课程，courseId：{}", courseId);
        courseBaseService.deleteCourse(courseId);
        log.info("删除课程成功，courseId：{}", courseId);
        return ContentResponse.success(null);
    }

    @Operation(summary = "发布课程")
    @PostMapping("/{courseId}/publish")
    public ContentResponse<Void> publishCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("发布课程，courseId：{}", courseId);
        courseBaseService.publishCourse(courseId);
        log.info("发布课程成功，courseId：{}", courseId);
        return ContentResponse.success(null);
    }

    @Operation(summary = "下架课程")
    @PostMapping("/{courseId}/offline")
    public ContentResponse<Void> offlineCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("下架课程，courseId：{}", courseId);
        courseBaseService.offlineCourse(courseId);
        log.info("下架课程成功，courseId：{}", courseId);
        return ContentResponse.success(null);
    }
}