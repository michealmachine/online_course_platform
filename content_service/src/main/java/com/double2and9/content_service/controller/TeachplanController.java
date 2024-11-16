package com.double2and9.content_service.controller;

import com.double2and9.content_service.common.model.ContentResponse;
import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.TeachplanDTO;
import com.double2and9.content_service.dto.TeachplanMediaDTO;
import com.double2and9.content_service.service.TeachplanService;
import com.double2and9.content_service.service.TeachplanMediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

/**
 * 课程计划管理接口
 */
@Tag(name = "课程计划管理", description = "提供课程计划的增删改查接口")
@Slf4j
@RestController
@RequestMapping("/teachplan")
public class TeachplanController {

    private final TeachplanService teachplanService;
    private final TeachplanMediaService teachplanMediaService;

    public TeachplanController(TeachplanService teachplanService,
                             TeachplanMediaService teachplanMediaService) {
        this.teachplanService = teachplanService;
        this.teachplanMediaService = teachplanMediaService;
    }

    /**
     * 查询课程计划树形结构
     * @param courseId 课程id
     * @return 课程计划树形结构
     */
    @Operation(summary = "查询课程计划树", description = "根据课程ID查询课程计划的树形结构")
    @GetMapping("/tree/{courseId}")
    public ContentResponse<List<TeachplanDTO>> getTeachplanTree(
            @Parameter(description = "课程ID", required = true) 
            @PathVariable Long courseId) {
        log.info("查询课程计划树，课程ID：{}", courseId);
        return ContentResponse.success(teachplanService.findTeachplanTree(courseId));
    }

    /**
     * 创建或修改课程计划
     */
    @Operation(summary = "保存课程计划", description = "创建或更新课程计划")
    @PostMapping
    public ContentResponse<Void> saveTeachplan(
            @Parameter(description = "课程计划信息", required = true)
            @RequestBody @Validated SaveTeachplanDTO teachplanDTO) {
        log.info("保存课程计划：{}", teachplanDTO);
        teachplanService.saveTeachplan(teachplanDTO);
        return ContentResponse.success(null);
    }

    /**
     * 删除课程计划
     */
    @Operation(summary = "删除课程计划")
    @DeleteMapping("/{teachplanId}")
    public ContentResponse<Void> deleteTeachplan(
            @Parameter(description = "课程计划ID", required = true) 
            @PathVariable Long teachplanId) {
        log.info("删除课程计划，ID：{}", teachplanId);
        teachplanService.deleteTeachplan(teachplanId);
        return ContentResponse.success(null);
    }

    /**
     * 课程计划向上移动
     * @param teachplanId 课程计划ID
     * @return 操作结果
     */
    @Operation(summary = "上移课程计划")
    @PostMapping("/moveup/{teachplanId}")
    public ContentResponse<Void> moveUp(
            @Parameter(description = "课程计划ID", required = true) 
            @PathVariable Long teachplanId) {
        log.info("上移课程计划，ID：{}", teachplanId);
        teachplanService.moveUp(teachplanId);
        return ContentResponse.success(null);
    }

    /**
     * 课程计划向下移动
     * @param teachplanId 课程计划ID
     * @return 操作结果
     */
    @Operation(summary = "下移课程计划")
    @PostMapping("/movedown/{teachplanId}")
    public ContentResponse<Void> moveDown(
            @Parameter(description = "课程计划ID", required = true) 
            @PathVariable Long teachplanId) {
        log.info("下移课程计划，ID：{}", teachplanId);
        teachplanService.moveDown(teachplanId);
        return ContentResponse.success(null);
    }

    @Operation(summary = "绑定媒资")
    @PostMapping("/media")
    public ContentResponse<Void> associateMedia(
            @Parameter(description = "媒资绑定信息", required = true)
            @RequestBody @Validated TeachplanMediaDTO teachplanMediaDTO) {
        log.info("绑定媒资：{}", teachplanMediaDTO);
        teachplanMediaService.associateMedia(teachplanMediaDTO);
        return ContentResponse.success(null);
    }

    @Operation(summary = "解除媒资绑定")
    @DeleteMapping("/media/{teachplanId}/{mediaId}")
    public ContentResponse<Void> dissociateMedia(
            @Parameter(description = "课程计划ID", required = true) 
            @PathVariable Long teachplanId,
            @Parameter(description = "媒资ID", required = true) 
            @PathVariable Long mediaId) {
        log.info("解除媒资绑定，课程计划ID：{}，媒资ID：{}", teachplanId, mediaId);
        teachplanMediaService.dissociateMedia(teachplanId, mediaId);
        return ContentResponse.success(null);
    }
} 