package com.double2and9.media.controller;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.common.model.MediaResponse;
import com.double2and9.media.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.util.Map;

@Tag(name = "媒资文件管理", description = "媒资文件管理相关接口")
@Slf4j
@RestController
@RequestMapping("/media")
public class MediaFileController {

    private final ImageService imageService;

    public MediaFileController(ImageService imageService) {
        this.imageService = imageService;
    }

    @Operation(summary = "上传图片到临时存储")
    @PostMapping("/images/temp")
    public MediaResponse<String> uploadImageTemp(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaException(MediaErrorCode.PARAM_ERROR, "文件不能为空");
        }
        return MediaResponse.success(imageService.uploadImageTemp(file));
    }
    @Operation(summary = "更新临时存储的图片")
    @PutMapping("/temp/{tempKey}")
    public MediaResponse<String> updateTemp(
            @PathVariable String tempKey,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaException(MediaErrorCode.PARAM_ERROR, "文件不能为空");
        }
        return MediaResponse.success(imageService.updateTemp(tempKey, file));
    }
    @Operation(summary = "保存临时文件到永久存储")
    @PostMapping("/temp/save")
    public MediaResponse<String> saveTempFile(@RequestBody Map<String, String> params) {
        String tempKey = params.get("tempKey");
        if (StringUtils.isEmpty(tempKey)) {
            throw new MediaException(MediaErrorCode.PARAM_ERROR, "临时文件key不能为空");
        }
        return MediaResponse.success(imageService.saveTempFile(tempKey));
    }
} 