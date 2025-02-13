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

    /**
     * 获取课程详情
     * 返回的 CourseBaseDTO 中包含:
     * - 基本信息(名称、简介等)
     * - 课程状态(status)
     * - 审核状态(auditStatus)
     * - 分类信息(大类/小类名称)
     * 
     * 权限:
     * - 机构用户: 只能查看本机构的课程
     * - 管理员: 可查看任意课程
     * 
     * 业务规则:
     * 1. 课程不存在时会抛出 COURSE_NOT_EXISTS 异常
     * 2. 返回的 DTO 包含最新的课程状态和审核状态
     * 3. 可用于课程编辑前的数据获取
     * 
     * @param courseId 课程ID
     * @return 课程详细信息DTO
     * @throws ContentException 当课程不存在时
     */
    @Operation(summary = "获取课程详情")
    @GetMapping("/{courseId}")
    public ContentResponse<CourseBaseDTO> getCourseById(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("获取课程详情，courseId：{}", courseId);
        CourseBaseDTO courseBaseDTO = courseBaseService.getCourseById(courseId);
        log.info("获取课程详情成功，courseId：{}", courseId);
        return ContentResponse.success(courseBaseDTO);
    }

    /**
     * 删除课程
     * 支持普通删除和强制删除两种模式
     * 
     * 权限:
     * - 机构用户: 只能删除本机构的课程
     * - 管理员: 可删除任意课程
     * 
     * 业务规则:
     * 1. 已发布的课程不能删除
     * 2. 普通删除时,如果课程存在教师关联会失败
     * 3. 强制删除时,会先解除所有教师关联再删除课程
     * 4. 删除课程时会同步删除:
     *    - 课程封面
     *    - 课程计划
     *    - 教师关联关系(强制删除时)
     * 
     * @param courseId 课程ID
     * @param force 是否强制删除
     * @throws ContentException 当课程不存在、状态错误或存在教师关联时
     */
    @Operation(summary = "删除课程")
    @DeleteMapping("/{courseId}")
    public ContentResponse<Void> deleteCourse(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "是否强制删除") @RequestParam(required = false) Boolean force) {
        log.info("删除课程，courseId：{}，force：{}", courseId, force);
        
        if (Boolean.TRUE.equals(force)) {
            courseBaseService.deleteCourseWithRelations(courseId, true);
        } else {
            courseBaseService.deleteCourse(courseId);
        }
        
        return ContentResponse.success(null);
    }

    /**
     * 发布课程
     * 将审核通过的课程变更为发布状态
     * 
     * 权限:
     * - 机构用户: 只能发布本机构的课程
     * - 管理员: 可发布任意课程
     * 
     * 业务规则:
     * 1. 只有审核通过的课程才能发布
     * 2. 已发布的课程不能重复发布
     * 3. 发布后课程状态变更为 PUBLISHED
     * 4. 发布时会创建课程发布记录
     * 
     * @param courseId 课程ID
     * @throws ContentException 当课程不存在、未审核通过或已发布时
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
     * 将已发布的课程变更为下线状态
     * 
     * 权限:
     * - 机构用户: 只能下架本机构的课程
     * - 管理员: 可下架任意课程
     * 
     * 业务规则:
     * 1. 只有已发布的课程可以下架
     * 2. 下架后课程状态变更为 OFFLINE
     * 3. 下架后课程发布记录状态同步更新
     * 
     * @param courseId 课程ID
     * @throws ContentException 当课程不存在或状态错误时
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
     * 支持按状态筛选的分页查询
     * 
     * 权限:
     * - 机构用户: 只能查询本机构的课程
     * 
     * 业务规则:
     * 1. 支持按课程状态筛选
     * 2. 分页查询,避免数据量过大
     * 3. 返回结果包含:
     *    - 课程基本信息
     *    - 课程状态
     *    - 审核状态
     *    - 分类信息
     * 
     * @param organizationId 机构ID
     * @param status 课程状态(可选)
     * @param pageParams 分页参数
     * @return 分页的课程列表
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
     * 上传课程封面(两步式上传第一步)
     * 将封面图片上传到临时存储
     * 
     * 权限:
     * - 机构用户: 只能为本机构的课程上传封面
     * 
     * 业务规则:
     * 1. 仅支持图片格式文件
     * 2. 文件先保存到临时存储
     * 3. 需要调用确认接口才会永久保存
     * 4. 临时文件有效期有限
     * 
     * @param courseId 课程ID
     * @param file 封面图片文件
     * @return 临时存储的key
     * @throws ContentException 当课程不存在或上传失败时
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
     * 确认课程封面(两步式上传第二步)
     * 将临时存储的封面图片转为永久存储
     * 
     * 权限:
     * - 机构用户: 只能为本机构的课程确认封面
     * 
     * 业务规则:
     * 1. 临时文件必须存在且未过期
     * 2. 确认后文件转为永久存储
     * 3. 原有封面文件会被删除
     * 4. 更新课程的封面URL
     * 
     * @param courseId 课程ID
     * @param tempKey 临时存储key
     * @throws ContentException 当课程不存在、临时文件不存在或确认失败时
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
}