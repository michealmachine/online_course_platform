package com.double2and9.content_service.controller;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.model.ContentResponse;
import com.double2and9.content_service.dto.TeachplanMediaDTO;
import com.double2and9.content_service.service.TeachplanMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课程计划媒资管理", description = "提供课程计划与媒资的关联管理接口")
@Slf4j
@RestController
@RequestMapping("/teachplan-media")
public class TeachplanMediaController {

    private final TeachplanMediaService teachplanMediaService;

    public TeachplanMediaController(TeachplanMediaService teachplanMediaService) {
        this.teachplanMediaService = teachplanMediaService;
    }

    @Operation(summary = "绑定课程计划与媒资")
    @PostMapping
    public ContentResponse<Void> associateMedia(
            @Parameter(description = "媒资绑定信息")
            @RequestBody @Validated TeachplanMediaDTO teachplanMediaDTO) {
        teachplanMediaService.associateMedia(teachplanMediaDTO);
        return ContentResponse.success(null);
    }

    @Operation(summary = "解除课程计划与媒资的绑定")
    @DeleteMapping("/{teachplanId}/{mediaId}")
    public ContentResponse<Void> dissociateMedia(
            @Parameter(description = "课程计划ID") @PathVariable Long teachplanId,
            @Parameter(description = "媒资ID") @PathVariable Long mediaId) {
        teachplanMediaService.dissociateMedia(teachplanId, String.valueOf(mediaId));
        return ContentResponse.success(null);
    }

    @Operation(summary = "获取课程计划关联的媒资列表")
    @GetMapping("/{teachplanId}")
    public ContentResponse<List<TeachplanMediaDTO>> getMediaList(
            @Parameter(description = "课程计划ID") @PathVariable Long teachplanId) {
        return ContentResponse.success(teachplanMediaService.getMediaByTeachplanId(teachplanId));
    }
}