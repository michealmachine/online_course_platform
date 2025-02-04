package com.double2and9.content_service.controller;

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

@Slf4j
@RestController
@RequestMapping("/course-teacher")
@Tag(name = "课程教师管理", description = "提供课程教师的增删改查接口")
public class CourseTeacherController {

    private final CourseTeacherService courseTeacherService;

    public CourseTeacherController(CourseTeacherService courseTeacherService) {
        this.courseTeacherService = courseTeacherService;
    }

    /**
     * 查询课程教师列表
     */
    @Operation(summary = "查询课程教师列表", description = "根据课程ID查询关联的教师列表")
    @GetMapping("/list/{courseId}")
    public ContentResponse<List<CourseTeacherDTO>> listByCourseId(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId) {
        log.info("查询课程教师列表，课程ID：{}", courseId);
        return ContentResponse.success(courseTeacherService.listByCourseId(courseId));
    }

    /**
     * 查询教师详情
     */
    @Operation(summary = "查询教师详情", description = "根据教师ID查询详细信息")
    @GetMapping("/{organizationId}/{teacherId}")
    public ContentResponse<CourseTeacherDTO> getTeacherDetail(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId,
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId) {
        log.info("查询教师详情，机构ID：{}，教师ID：{}", organizationId, teacherId);
        return ContentResponse.success(courseTeacherService.getTeacherDetail(organizationId, teacherId));
    }

    /**
     * 查询教师关联的课程列表
     */
    @Operation(summary = "查询教师关联的课程", description = "根据教师ID查询其关联的所有课程")
    @GetMapping("/courses/{teacherId}")
    public ContentResponse<List<CourseBaseDTO>> listCoursesByTeacherId(
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId) {
        log.info("查询教师关联的课程，教师ID：{}", teacherId);
        return ContentResponse.success(courseTeacherService.listCoursesByTeacherId(teacherId));
    }

    /**
     * 查询机构下的所有教师
     */
    @Operation(summary = "查询机构教师列表", description = "根据机构ID查询该机构下的所有教师")
    @GetMapping("/organization/{organizationId}")
    public ContentResponse<List<CourseTeacherDTO>> listByOrganizationId(
            @Parameter(description = "机构ID", required = true) @PathVariable Long organizationId) {
        log.info("查询机构教师列表，机构ID：{}", organizationId);
        return ContentResponse.success(courseTeacherService.listByOrganizationId(organizationId));
    }

    /**
     * 添加或修改课程教师
     */
    @Operation(summary = "保存课程教师信息", description = "创建或更新教师信息，同时管理与课程的关联关系")
    @PostMapping
    public ContentResponse<Void> saveCourseTeacher(
            @Parameter(description = "教师信息", required = true) @RequestBody @Validated SaveCourseTeacherDTO teacherDTO) {
        log.info("保存课程教师信息：{}", teacherDTO);
        courseTeacherService.saveCourseTeacher(teacherDTO);
        return ContentResponse.success(null);
    }

    /**
     * 解除教师与课程的关联关系
     */
    @Operation(summary = "解除教师与课程的关联", description = "解除特定教师与课程的关联关系，如果教师不再关联任何课程则删除教师")
    @DeleteMapping("/{courseId}/{teacherId}")
    public ContentResponse<Void> deleteCourseTeacher(
            @Parameter(description = "课程ID", required = true) @PathVariable Long courseId,
            @Parameter(description = "教师ID", required = true) @PathVariable Long teacherId) {
        log.info("解除教师与课程的关联，课程ID：{}，教师ID：{}", courseId, teacherId);
        courseTeacherService.deleteCourseTeacher(courseId, teacherId);
        return ContentResponse.success(null);
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