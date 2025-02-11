package com.double2and9.content_service.controller;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.model.ContentResponse;
import com.double2and9.content_service.dto.CourseAuditDTO;
import com.double2and9.content_service.dto.CoursePublishPreDTO;
import com.double2and9.content_service.dto.CourseAuditHistoryDTO;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.service.CourseAuditService;
import com.double2and9.content_service.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "课程审核接口")
@RestController
@RequestMapping("/course-audit")
public class CourseAuditController {

    private final CourseAuditService courseAuditService;

    public CourseAuditController(CourseAuditService courseAuditService) {
        this.courseAuditService = courseAuditService;
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
}