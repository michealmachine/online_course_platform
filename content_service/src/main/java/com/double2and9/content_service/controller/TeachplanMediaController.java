package com.double2and9.content_service.controller;

import com.double2and9.content_service.dto.TeachplanMediaDTO;
import com.double2and9.content_service.service.TeachplanMediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/teachplan-media")
public class TeachplanMediaController {

    private final TeachplanMediaService teachplanMediaService;

    public TeachplanMediaController(TeachplanMediaService teachplanMediaService) {
        this.teachplanMediaService = teachplanMediaService;
    }

    /**
     * 绑定课程计划与媒资
     */
    @PostMapping
    public void associateMedia(@RequestBody @Validated TeachplanMediaDTO teachplanMediaDTO) {
        teachplanMediaService.associateMedia(teachplanMediaDTO);
    }

    /**
     * 解除绑定
     */
    @DeleteMapping("/{teachplanId}/{mediaId}")
    public void dissociateMedia(@PathVariable Long teachplanId, @PathVariable Long mediaId) {
        teachplanMediaService.dissociateMedia(teachplanId, mediaId);
    }

    /**
     * 获取课程计划关联的媒资列表
     */
    @GetMapping("/{teachplanId}")
    public List<TeachplanMediaDTO> getMediaList(@PathVariable Long teachplanId) {
        return teachplanMediaService.getMediaByTeachplanId(teachplanId);
    }
} 