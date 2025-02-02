package com.double2and9.content_service.client;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体服务Feign客户端降级处理
 * 当media服务不可用时的降级处理
 */
@Component
public class MediaFeignClientFallback implements MediaFeignClient {

    /**
     * 删除媒体文件的降级处理
     * 返回服务不可用的错误响应
     * 
     * @param url 文件URL
     * @return 错误响应对象
     */
    @Override
    public CommonResponse<?> deleteMediaFile(String url) {
        return CommonResponse.error("500", "媒体服务不可用");
    }

    /**
     * 上传课程封面的降级处理
     * 返回服务不可用的错误响应
     * 
     * @param courseId       课程ID
     * @param organizationId 机构ID
     * @param file           文件
     * @return 错误响应对象
     */
    @Override
    public CommonResponse<MediaFileDTO> uploadCourseLogo(Long courseId, Long organizationId, MultipartFile file) {
        return CommonResponse.error("500", "媒体服务不可用");
    }
}