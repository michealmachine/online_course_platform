package com.double2and9.content_service.controller;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.model.ContentResponse;
import com.double2and9.content_service.dto.*;
import com.double2and9.content_service.service.CourseAuditService;
import com.double2and9.content_service.service.CourseBaseService;
import com.double2and9.content_service.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "课程审核接口")
@RestController
@RequestMapping("/course-audit")
@Slf4j
public class CourseAuditController {

    private final CourseAuditService courseAuditService;
    private final CourseBaseService courseBaseService;

    public CourseAuditController(CourseAuditService courseAuditService, 
                                CourseBaseService courseBaseService) {
        this.courseAuditService = courseAuditService;
        this.courseBaseService = courseBaseService;
    }

    @Operation(summary = "提交课程审核")
    @PostMapping("/submit/{courseId}")
    public ContentResponse<Void> submitForAudit(@PathVariable Long courseId) {
        courseAuditService.submitForAudit(courseId);
        return ContentResponse.success(null);
    }

    @Operation(summary = "审核课程")
    @PostMapping("/approve")
    public ContentResponse<Void> auditCourse(@RequestBody CourseAuditDTO auditDTO) {
        // 从token中获取当前用户ID
        Long currentUserId = SecurityUtils.getCurrentUserId();
        auditDTO.setAuditorId(currentUserId);

        courseAuditService.auditCourse(auditDTO);
        return ContentResponse.success(null);
    }

    @Operation(summary = "获取待审核课程列表")
    @GetMapping("/pending")
    public ContentResponse<PageResult<CourseBaseDTO>> getPendingAuditCourses(PageParams pageParams) {
        return ContentResponse.success(courseAuditService.getPendingAuditCourses(pageParams));
    }

    @Operation(summary = "获取课程审核历史")
    @GetMapping("/history/{courseId}")
    public ContentResponse<PageResult<CourseAuditHistoryDTO>> getAuditHistory(
            @PathVariable Long courseId,
            PageParams pageParams) {
        return ContentResponse.success(courseAuditService.getAuditHistory(courseId, pageParams));
    }

    @Operation(summary = "获取审核人的审核历史")
    @GetMapping("/history/auditor/{auditorId}")
    public ContentResponse<PageResult<CourseAuditHistoryDTO>> getAuditorHistory(
            @PathVariable Long auditorId,
            PageParams pageParams) {
        return ContentResponse.success(courseAuditService.getAuditorHistory(auditorId, pageParams));
    }

    /**
     * 获取课程审核状态
     * 用于机构查询自己课程的审核进度
     * 
     * 权限:
     * - 机构用户: 只能查询本机构的课程
     * - 管理员: 可查询任意课程
     * 
     * @param courseId 课程ID
     * @param organizationId 当前用户所属机构ID
     * @return 审核状态码
     */
    @Operation(summary = "获取课程审核状态")
    @GetMapping("/status/{courseId}")
    public ContentResponse<String> getAuditStatus(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "机构ID", required = true) @RequestParam Long organizationId) {
        
        log.info("获取课程审核状态，courseId：{}，organizationId：{}", courseId, organizationId);
        String auditStatus = courseBaseService.getAuditStatus(courseId, organizationId);
        log.info("获取课程审核状态成功，courseId：{}，status：{}", courseId, auditStatus);
        return ContentResponse.success(auditStatus);
    }

    /**
     * 查询已通过审核的课程列表
     * 仅返回已审核通过的课程，用于前台展示
     */
    @Operation(summary = "查询已通过审核的课程列表")
    @GetMapping("/approved")
    public ContentResponse<PageResult<CourseBaseDTO>> listApprovedCourses(
            @Parameter(description = "分页参数") PageParams pageParams,
            @Parameter(description = "查询条件") QueryCourseParamsDTO queryParams) {
        return ContentResponse.success(courseBaseService.queryApprovedCourseList(pageParams, queryParams));
    }

    /**
     * 获取待审核课程详情
     * 返回课程的完整信息，包括：
     * - 基本信息
     * - 课程计划
     * - 教师信息
     * - 审核状态
     * 
     * 权限:
     * - 审核人员: 可查看所有待审核课程
     * - 管理员: 可查看所有课程
     * 
     * @param courseId 课程ID
     * @return 课程预览信息
     */
    @Operation(summary = "获取待审核课程详情")
    @GetMapping("/detail/{courseId}")
    public ContentResponse<CoursePreviewDTO> getAuditCourseDetail(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("获取待审核课程详情，courseId：{}", courseId);
        CoursePreviewDTO previewInfo = courseBaseService.preview(courseId);
        log.info("获取待审核课程详情成功，courseId：{}", courseId);
        return ContentResponse.success(previewInfo);
    }

    /**
     * 管理员查询所有课程
     * 支持多条件筛选，包括机构、状态、审核状态等
     * 
     * 权限:
     * - 仅审核人员和管理员可访问
     * 
     * 业务规则:
     * 1. 可按机构ID筛选
     * 2. 可按课程状态筛选
     * 3. 可按审核状态筛选
     * 4. 可按课程名称模糊搜索
     * 
     * @param organizationId 机构ID(可选)
     * @param status 课程状态(可选)
     * @param auditStatus 审核状态(可选)
     * @param courseName 课程名称(可选)
     * @param pageParams 分页参数
     * @return 分页的课程列表
     */
    @Operation(summary = "管理员查询课程列表", description = "查询所有机构的课程列表，支持多条件筛选")
    @GetMapping("/courses")
    public ContentResponse<PageResult<CourseAuditInfoDTO>> queryCourses(
            @Parameter(description = "机构ID") @RequestParam(required = false) Long organizationId,
            @Parameter(description = "课程状态") @RequestParam(required = false) String status,
            @Parameter(description = "审核状态") @RequestParam(required = false) String auditStatus,
            @Parameter(description = "课程名称") @RequestParam(required = false) String courseName,
            @Parameter(description = "分页参数") PageParams pageParams) {

        log.info("查询课程列表，机构ID：{}，状态：{}，审核状态：{}，课程名称：{}",
                organizationId, status, auditStatus, courseName);

        QueryCourseParamsDTO queryParams = new QueryCourseParamsDTO();
        queryParams.setOrganizationId(organizationId);
        queryParams.setStatus(status);
        queryParams.setAuditStatus(auditStatus);
        queryParams.setCourseName(courseName);

        return ContentResponse.success(
                courseAuditService.queryCourseAuditList(pageParams, queryParams));
    }
}