package com.double2and9.content_service.client;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体服务Feign客户端
 * 用于content服务调用media服务的接口
 */
@FeignClient(name = "media-service", fallback = MediaFeignClientFallback.class)
public interface MediaFeignClient {

    /**
     * 删除媒体文件
     * 
     * @param url 文件URL路径
     * @return 通用响应对象
     */
    @DeleteMapping("/media/files/{url}")
    CommonResponse<?> deleteMediaFile(@PathVariable("url") String url);

    /**
     * 上传课程封面图片
     * 
     * @param courseId       课程ID
     * @param organizationId 机构ID,用于权限验证
     * @param file           封面图片文件
     * @return 包含媒体文件信息的响应对象
     */
    @PostMapping("/media/files/course/{courseId}/logo")
    CommonResponse<MediaFileDTO> uploadCourseLogo(
            @PathVariable("courseId") Long courseId,
            @RequestParam("organizationId") Long organizationId,
            @RequestPart("file") MultipartFile file);
}