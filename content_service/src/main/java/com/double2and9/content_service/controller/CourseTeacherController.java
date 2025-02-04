package com.double2and9.content_service.controller;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.model.ContentResponse;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.dto.CourseTeacherDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import com.double2and9.content_service.service.CourseTeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程教师管理控制器
 * 权限说明：
 * 1. /admin/* 路径下的接口需要管理员权限
 * 2. /organization/* 路径下的接口需要机构权限且只能操作自己机构的数据
 * 
 * TODO: 机构ID验证将在实现认证授权后从Token中获取，当前通过路径参数传入仅用于开发测试
 */
@Slf4j
@RestController
@RequestMapping("/course-teacher")
@Tag(name = "课程教师管理", description = "提供教师管理和课程关联的接口")
public class CourseTeacherController {

    private final CourseTeacherService courseTeacherService;

    public CourseTeacherController(CourseTeacherService courseTeacherService) {
        this.courseTeacherService = courseTeacherService;
    }

    /**
     * 查询机构的教师列表
     * 权限要求：机构权限，只能查询自己机构的教师
     */
    @Operation(summary = "查询机构教师列表", description = "获取指定机构的所有教师")
    @GetMapping("/organization/{organizationId}/teachers")
    public ContentResponse<PageResult<CourseTeacherDTO>> listOrganizationTeachers(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId,
            @Parameter(description = "分页参数") PageParams pageParams) {
        log.info("查询机构教师列表，机构ID：{}，分页参数：{}", organizationId, pageParams);
        return ContentResponse.success(courseTeacherService.listByOrganizationId(organizationId, pageParams));
    }

    /**
     * 查询教师详情
     * 权限要求：机构权限，只能查询自己机构的教师
     */
    @Operation(summary = "查询教师详情")
    @GetMapping("/organization/{organizationId}/teachers/{teacherId}")
    public ContentResponse<CourseTeacherDTO> getTeacherDetail(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId,
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId) {
        log.info("查询教师详情，机构ID：{}，教师ID：{}", organizationId, teacherId);
        return ContentResponse.success(courseTeacherService.getTeacherDetail(organizationId, teacherId));
    }

    /**
     * 添加/修改教师信息
     * 权限要求：机构权限，只能操作自己机构的教师
     */
    @Operation(summary = "保存教师信息", description = "创建或更新教师信息")
    @PostMapping("/organization/{organizationId}/teachers")
    public ContentResponse<Long> saveTeacher(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId,
            @Parameter(description = "教师信息", required = true) @RequestBody @Validated SaveCourseTeacherDTO teacherDTO) {
        log.info("保存教师信息，机构ID：{}，教师信息：{}", organizationId, teacherDTO);
        teacherDTO.setOrganizationId(organizationId); // 确保使用路径中的机构ID
        return ContentResponse.success(courseTeacherService.saveTeacher(teacherDTO));
    }

    /**
     * 删除教师
     * 权限要求：机构权限，只能删除自己机构的教师
     */
    @Operation(summary = "删除教师")
    @DeleteMapping("/organization/{organizationId}/teachers/{teacherId}")
    public ContentResponse<Void> deleteTeacher(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId,
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId) {
        log.info("删除教师，机构ID：{}，教师ID：{}", organizationId, teacherId);
        courseTeacherService.deleteTeacher(organizationId, teacherId);
        return ContentResponse.success(null);
    }

    /**
     * 关联教师到课程
     * 权限要求：机构权限，只能关联自己机构的教师和课程
     */
    @Operation(summary = "关联教师到课程")
    @PostMapping("/organization/{organizationId}/courses/{courseId}/teachers/{teacherId}")
    public ContentResponse<Void> associateTeacherToCourse(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId,
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId) {
        log.info("关联教师到课程，机构ID：{}，课程ID：{}，教师ID：{}", organizationId, courseId, teacherId);
        courseTeacherService.associateTeacherToCourse(organizationId, courseId, teacherId);
        return ContentResponse.success(null);
    }

    /**
     * 解除教师与课程的关联
     * 权限要求：机构权限，只能操作自己机构的教师和课程
     */
    @Operation(summary = "解除教师与课程的关联")
    @DeleteMapping("/organization/{organizationId}/courses/{courseId}/teachers/{teacherId}")
    public ContentResponse<Void> dissociateTeacherFromCourse(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId,
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId) {
        log.info("解除教师与课程的关联，机构ID：{}，课程ID：{}，教师ID：{}", organizationId, courseId, teacherId);
        courseTeacherService.dissociateTeacherFromCourse(organizationId, courseId, teacherId);
        return ContentResponse.success(null);
    }

    /**
     * 查询课程教师列表
     */
    @Operation(summary = "查询课程教师列表", description = "根据课程ID查询关联的教师列表")
    @GetMapping("/courses/{courseId}/teachers")
    public ContentResponse<PageResult<CourseTeacherDTO>> listByCourseId(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "分页参数") PageParams pageParams) {
        log.info("查询课程教师列表，课程ID：{}", courseId);
        return ContentResponse.success(courseTeacherService.listByCourseId(courseId, pageParams));
    }

    /**
     * 查询教师关联的课程列表
     */
    @Operation(summary = "查询教师关联的课程", description = "根据教师ID查询其关联的所有课程")
    @GetMapping("/teachers/{teacherId}/courses")
    public ContentResponse<PageResult<CourseBaseDTO>> listCoursesByTeacherId(
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId,
            @Parameter(description = "分页参数") PageParams pageParams) {
        log.info("查询教师关联的课程，教师ID：{}", teacherId);
        return ContentResponse.success(courseTeacherService.listCoursesByTeacherId(teacherId, pageParams));
    }

    /**
     * 上传教师头像到临时存储
     */
    @Operation(summary = "上传教师头像到临时存储")
    @PostMapping("/{teacherId}/avatar/temp")
    public ContentResponse<String> uploadTeacherAvatarTemp(
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId,
            @Parameter(description = "头像图片文件", required = true) @RequestPart("file") MultipartFile file) {
        log.info("上传教师头像到临时存储，teacherId：{}", teacherId);
        String tempKey = courseTeacherService.uploadTeacherAvatarTemp(teacherId, file);
        log.info("上传教师头像到临时存储成功，teacherId：{}，tempKey：{}", teacherId, tempKey);
        return ContentResponse.success(tempKey);
    }

    /**
     * 确认并保存临时头像
     */
    @Operation(summary = "确认并保存临时头像")
    @PostMapping("/{teacherId}/avatar/confirm")
    public ContentResponse<Void> confirmTeacherAvatar(
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId,
            @Parameter(description = "临时存储key", required = true) @RequestParam String tempKey) {
        log.info("确认保存教师头像，teacherId：{}，tempKey：{}", teacherId, tempKey);
        courseTeacherService.confirmTeacherAvatar(teacherId, tempKey);
        log.info("确认保存教师头像成功，teacherId：{}", teacherId);
        return ContentResponse.success(null);
    }

    /**
     * 删除教师头像
     */
    @Operation(summary = "删除教师头像")
    @DeleteMapping("/{teacherId}/avatar")
    public ContentResponse<Void> deleteTeacherAvatar(
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId) {
        log.info("删除教师头像，teacherId：{}", teacherId);
        courseTeacherService.deleteTeacherAvatar(teacherId);
        log.info("删除教师头像成功，teacherId：{}", teacherId);
        return ContentResponse.success(null);
    }
}