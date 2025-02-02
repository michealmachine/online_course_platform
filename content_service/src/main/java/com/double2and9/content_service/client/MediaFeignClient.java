package com.double2and9.content_service.client;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.enums.ContentErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体服务Feign客户端
 * 用于content服务调用media服务的接口
 */
@FeignClient(name = "media-service")
public interface MediaFeignClient {

    /**
     * 删除媒体文件
     * 
     * @param url 文件URL路径
     * @return 通用响应对象
     */
    @DeleteMapping("/media/files/{url}")
    @CircuitBreaker(name = "backendA", fallbackMethod = "deleteMediaFileFallback")
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
    @CircuitBreaker(name = "backendA", fallbackMethod = "uploadCourseLogoFallback")
    CommonResponse<MediaFileDTO> uploadCourseLogo(
            @PathVariable("courseId") Long courseId,
            @RequestParam("organizationId") Long organizationId,
            @RequestPart("file") MultipartFile file);

    /**
     * 上传课程封面的降级方法
     */
    default CommonResponse<MediaFileDTO> uploadCourseLogoFallback(
            Long courseId, Long organizationId, MultipartFile file, Throwable throwable) {
        return CommonResponse.error(String.valueOf(ContentErrorCode.UPLOAD_LOGO_FAILED.getCode()),
                ContentErrorCode.UPLOAD_LOGO_FAILED.getMessage());
    }

    /**
     * 删除媒体文件的降级方法
     */
    default CommonResponse<?> deleteMediaFileFallback(String url, Throwable throwable) {
        return CommonResponse.error(String.valueOf(ContentErrorCode.DELETE_LOGO_FAILED.getCode()),
                ContentErrorCode.DELETE_LOGO_FAILED.getMessage());
    }
}