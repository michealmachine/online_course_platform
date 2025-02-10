package com.double2and9.content_service.controller;

import com.double2and9.base.enums.CourseStatusEnum;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.dto.QueryCourseParamsDTO;
import com.double2and9.content_service.dto.CourseCategoryTreeDTO;
import com.double2and9.content_service.dto.AddCourseDTO;
import com.double2and9.content_service.dto.EditCourseDTO;
import com.double2and9.content_service.dto.CoursePreviewDTO;
import com.double2and9.content_service.service.CourseBaseService;
import com.double2and9.content_service.common.model.ContentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import com.double2and9.base.enums.ContentErrorCode;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 课程管理控制器
 * 提供课程相关的REST API接口,包括:
 * - 课程基本信息的CRUD操作
 * - 课程封面管理
 * - 课程发布和下架
 * 
 * 权限说明：
 * 1. 管理员权限接口 (/admin/*)
 * - 可以查看和操作所有机构的课程
 * 
 * 2. 机构权限接口 (/organization/*)
 * - 只能操作自己机构的课程
 */
@Tag(name = "课程管理", description = "提供课程的增删改查、发布等接口")
@Slf4j
@RestController
@RequestMapping("/course")
public class CourseController {

    private final CourseBaseService courseBaseService;

    public CourseController(CourseBaseService courseBaseService) {
        this.courseBaseService = courseBaseService;
    }

    /**
     * 创建课程
     * 权限：机构用户（只能为自己机构创建课程）
     */
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

    /**
     * 发布课程
     * 权限：机构用户（只能发布本机构的已审核通过的课程）
     */
    @Operation(summary = "发布课程")
    @PostMapping("/{courseId}/publish")
    public ContentResponse<Void> publishCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("发布课程，courseId：{}", courseId);
        courseBaseService.publishCourse(courseId);
        log.info("发布课程成功，courseId：{}", courseId);
        return ContentResponse.success(null);
    }

    /**
     * 下架课程
     * 权限：
     * - 机构用户：只能下架本机构的课程
     * - 管理员：可下架任意课程
     */
    @Operation(summary = "下架课程")
    @PostMapping("/{courseId}/offline")
    public ContentResponse<Void> offlineCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("下架课程，courseId：{}", courseId);
        courseBaseService.offlineCourse(courseId);
        log.info("下架课程成功，courseId：{}", courseId);
        return ContentResponse.success(null);
    }

    /**
     * 查询机构课程列表
     * 权限：
     * - 机构用户：只能查询本机构课程
     */
    @Operation(summary = "查询机构课程列表", description = "查询指定机构的课程列表，支持状态筛选")
    @GetMapping("/organization/{organizationId}")
    public ContentResponse<PageResult<CourseBaseDTO>> queryCourseList(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId,
            @Parameter(description = "课程状态") @RequestParam(required = false) String status,
            @Parameter(description = "分页参数") PageParams pageParams) {

        log.info("查询机构课程列表，机构ID：{}，状态：{}", organizationId, status);

        // 构建查询参数
        QueryCourseParamsDTO queryParams = new QueryCourseParamsDTO();
        queryParams.setStatus(status);

        // 使用 queryCourseListByOrg 方法,强制限制机构ID
        return ContentResponse.success(
                courseBaseService.queryCourseListByOrg(organizationId, pageParams, queryParams));
    }

    /**
     * 上传课程封面到临时存储
     * 实现两步式上传的第一步,将文件先保存到临时存储
     *
     * @param courseId 课程ID
     * @param file     封面图片文件
     * @return 临时存储的key
     * @throws ContentException 当课程不存在或上传失败时
     *                          上传课程封面（两步式上传）
     *                          权限：机构用户（只能为本机构课程上传封面）
     */
    @Operation(summary = "上传课程封面到临时存储")
    @PostMapping("/{courseId}/logo/temp")
    public ContentResponse<String> uploadCourseLogoTemp(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "封面图片文件", required = true) @RequestPart("file") MultipartFile file) {
        log.info("上传课程封面到临时存储，courseId：{}", courseId);
        String tempKey = courseBaseService.uploadCourseLogoTemp(courseId, file);
        log.info("上传课程封面到临时存储成功，courseId：{}，tempKey：{}", courseId, tempKey);
        return ContentResponse.success(tempKey);
    }

    /**
     * 确认并保存临时课程封面
     * 实现两步式上传的第二步,将临时文件转存为永久文件
     *
     * @param courseId 课程ID
     * @param tempKey  临时存储key
     * @throws ContentException 当课程不存在、临时文件不存在或保存失败时
     *                          确认课程封面
     *                          权限：机构用户（只能为本机构课程确认封面）
     * 
     */
    @Operation(summary = "确认并保存临时课程封面")
    @PostMapping("/{courseId}/logo/confirm")
    public ContentResponse<Void> confirmCourseLogo(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "临时存储key", required = true) @RequestParam String tempKey) {
        log.info("确认保存课程封面，courseId：{}，tempKey：{}", courseId, tempKey);
        courseBaseService.confirmCourseLogo(courseId, tempKey);
        log.info("确认保存课程封面成功，courseId：{}", courseId);
        return ContentResponse.success(null);
    }

    /**
     * 删除课程封面
     * 同时删除媒体服务中的文件和课程中的封面引用
     *
     * @param courseId 课程ID
     * @throws ContentException 当课程不存在或删除失败时
     *                          权限：机构用户（只能删除本机构课程的封面）
     */
    @Operation(summary = "删除课程封面")
    @DeleteMapping("/{courseId}/logo")
    public ContentResponse<Void> deleteCourseLogo(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("删除课程封面，courseId：{}", courseId);
        courseBaseService.deleteCourseLogo(courseId);
        log.info("删除课程封面成功，courseId：{}", courseId);
        return ContentResponse.success(null);
    }

    /**
     * 管理员查询所有课程
     * 权限：仅管理员
     */
    @Operation(summary = "管理员查询课程列表", description = "管理员查询所有机构的课程列表，支持多条件筛选")
    @GetMapping("/admin/list")
    public ContentResponse<PageResult<CourseBaseDTO>> queryAllCourses(
            @Parameter(description = "机构ID") @RequestParam(required = false) Long organizationId,
            @Parameter(description = "课程状态") @RequestParam(required = false) String status,
            @Parameter(description = "审核状态") @RequestParam(required = false) String auditStatus,
            @Parameter(description = "课程名称") @RequestParam(required = false) String courseName,
            @Parameter(description = "分页参数") PageParams pageParams) {

        log.info("管理员查询课程列表，机构ID：{}，状态：{}，审核状态：{}，课程名称：{}",
                organizationId, status, auditStatus, courseName);

        QueryCourseParamsDTO queryParams = new QueryCourseParamsDTO();
        queryParams.setOrganizationId(organizationId);
        queryParams.setStatus(status);
        queryParams.setAuditStatus(auditStatus);
        queryParams.setCourseName(courseName);

        return ContentResponse.success(
                courseBaseService.queryCourseList(pageParams, queryParams));
    }

    /**
     * 查询已通过审核的课程列表
     */
    @Operation(summary = "查询已通过审核的课程列表")
    @GetMapping("/list")
    public ContentResponse<PageResult<CourseBaseDTO>> listApprovedCourses(
            @Parameter(description = "分页参数") PageParams pageParams,
            @Parameter(description = "查询条件") QueryCourseParamsDTO queryParams) {
        return ContentResponse.success(courseBaseService.queryApprovedCourseList(pageParams, queryParams));
    }
}