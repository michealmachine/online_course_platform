package com.double2and9.media.controller;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.entity.MediaFile;
import com.double2and9.media.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "媒资文件管理", description = "媒资文件管理相关接口")
@Slf4j
@RestController
@RequestMapping("/api/media")
public class MediaFileController {

    private final ImageService imageService;
    private final ModelMapper modelMapper;

    public MediaFileController(ImageService imageService, ModelMapper modelMapper) {
        this.imageService = imageService;
        this.modelMapper = modelMapper;
    }

    @Operation(summary = "上传图片到临时存储")
    @PostMapping("/images/temp")
    public CommonResponse<String> uploadImageTemp(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaException(MediaErrorCode.PARAM_ERROR, "文件不能为空");
        }
        return CommonResponse.success(imageService.uploadImageTemp(file));
    }

    @Operation(summary = "更新临时存储的图片")
    @PutMapping("/temp/{tempKey}")
    public CommonResponse<String> updateTemp(
            @PathVariable String tempKey,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaException(MediaErrorCode.PARAM_ERROR, "文件不能为空");
        }
        return CommonResponse.success(imageService.updateTemp(tempKey, file));
    }

    @Operation(summary = "保存临时文件到永久存储")
    @PostMapping("/temp/save")
    public CommonResponse<MediaFileDTO> saveTempFile(@RequestBody Map<String, String> params) {
        String tempKey = params.get("tempKey");
        if (tempKey == null || tempKey.isBlank()) {
            throw new MediaException(MediaErrorCode.PARAM_ERROR, "临时文件key不能为空");
        }
        MediaFile mediaFile = imageService.saveTempFile(tempKey);
        return CommonResponse.success(modelMapper.map(mediaFile, MediaFileDTO.class));
    }

    @Operation(summary = "删除媒体文件")
    @DeleteMapping("/files")
    public CommonResponse<?> deleteMediaFile(@RequestParam("url") String url) {
        if (url == null || url.isBlank()) {
            throw new MediaException(MediaErrorCode.PARAM_ERROR, "文件URL不能为空");
        }
        imageService.deleteMediaFile(url);
        return CommonResponse.success(null);
    }
}