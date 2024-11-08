package com.double2and9.content_service.controller;

import com.double2and9.content_service.common.model.ContentResponse;
import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.TeachplanDTO;
import com.double2and9.content_service.service.TeachplanService;
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

    public TeachplanController(TeachplanService teachplanService) {
        this.teachplanService = teachplanService;
    }

    /**
     * 查询课程计划树形结构
     * @param courseId 课程id
     * @return 课程计划树形结构
     */
    @Operation(summary = "获取课程计划树形结构")
    @Parameter(name = "courseId", description = "课程ID", required = true)
    @GetMapping("/tree/{courseId}")
    public ContentResponse<List<TeachplanDTO>> getTreeNodes(@PathVariable Long courseId) {
        List<TeachplanDTO> teachplanTree = teachplanService.findTeachplanTree(courseId);
        return ContentResponse.success(teachplanTree);
    }

    /**
     * 创建或修改课程计划
     */
    @Operation(summary = "保存课程计划", description = "创建或更新课程计划")
    @PostMapping
    public ContentResponse<Void> saveTeachplan(@RequestBody @Validated SaveTeachplanDTO teachplanDTO) {
        teachplanService.saveTeachplan(teachplanDTO);
        return ContentResponse.success(null);
    }

    /**
     * 删除课程计划
     */
    @Operation(summary = "删除课程计划")
    @Parameter(name = "teachplanId", description = "课程计划ID", required = true)
    @DeleteMapping("/{teachplanId}")
    public ContentResponse<Void> deleteTeachplan(@PathVariable Long teachplanId) {
        teachplanService.deleteTeachplan(teachplanId);
        return ContentResponse.success(null);
    }

    /**
     * 课程计划向上移动
     * @param teachplanId 课程计划ID
     * @return 操作结果
     */
    @Operation(summary = "课程计划向上移动")
    @Parameter(name = "teachplanId", description = "课程计划ID", required = true)
    @PostMapping("/{teachplanId}/moveup")
    public ContentResponse<Void> moveUp(@PathVariable Long teachplanId) {
        log.info("课程计划向上移动，teachplanId: {}", teachplanId);
        teachplanService.moveUp(teachplanId);
        return ContentResponse.success(null);
    }

    /**
     * 课程计划向下移动
     * @param teachplanId 课程计划ID
     * @return 操作结果
     */
    @Operation(summary = "课程计划向下移动")
    @Parameter(name = "teachplanId", description = "课程计划ID", required = true)
    @PostMapping("/{teachplanId}/movedown")
    public ContentResponse<Void> moveDown(@PathVariable Long teachplanId) {
        log.info("课程计划向下移动，teachplanId: {}", teachplanId);
        teachplanService.moveDown(teachplanId);
        return ContentResponse.success(null);
    }
} 