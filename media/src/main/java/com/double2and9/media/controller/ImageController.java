package com.double2and9.media.controller;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.media.service.ImageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片管理控制器
 * 处理图片上传、删除等操作
 */
@RestController
@RequestMapping("/media/files")
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * 上传课程封面图片
     * 支持图片格式校验和文件去重
     * 
     * @param courseId       课程ID,用于生成文件标识
     * @param organizationId 机构ID,用于权限控制
     * @param file           封面图片文件
     * @return 包含媒体文件信息的响应对象
     */
    @PostMapping("/course/{courseId}/logo")
    public CommonResponse<MediaFileDTO> uploadCourseLogo(
            @PathVariable Long courseId,
            @RequestParam Long organizationId,
            @RequestPart MultipartFile file) {
        MediaFileDTO mediaFileDTO = imageService.uploadCourseLogo(organizationId, courseId, file);
        return CommonResponse.success(mediaFileDTO);
    }

    /**
     * 删除媒体文件
     * 同时删除MinIO中的文件和数据库记录
     * 
     * @param url 文件URL,用于定位文件
     * @return 通用响应对象
     */
    @DeleteMapping("/{url}")
    public CommonResponse<?> deleteMediaFile(@PathVariable String url) {
        imageService.deleteMediaFile(url);
        return CommonResponse.success(null);
    }
}