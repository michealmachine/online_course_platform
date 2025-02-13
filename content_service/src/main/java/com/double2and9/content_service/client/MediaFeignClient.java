package com.double2and9.content_service.client;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.config.FeignMultipartConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 媒体服务Feign客户端
 * 用于与媒体服务进行跨服务调用,主要处理:
 * - 文件上传(两步式)
 * - 文件删除
 * - 文件访问
 */

@FeignClient(
    name = "media-service",
    configuration = FeignMultipartConfig.class
)
public interface MediaFeignClient {

        /**
         * 上传图片到临时存储
         * 实现两步式上传的第一步
         *
         * @param file 要上传的文件
         * @return 包含临时存储key的响应对象
         */
        @PostMapping(value = "/api/media/images/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        CommonResponse<String> uploadImageTemp(@RequestPart("file") MultipartFile file);

        /**
         * 保存临时文件到永久存储
         * 实现两步式上传的第二步
         *
         * @param params 包含tempKey的参数Map
         * @return 包含媒体文件信息的响应对象
         */
        @PostMapping("/api/media/temp/save")
        CommonResponse<MediaFileDTO> saveTempFile(@RequestBody Map<String, String> params);

        /**
         * 删除媒体文件
         *
         * @param url 文件访问URL
         * @return 删除操作的响应对象
         */
        @DeleteMapping("/api/media/files")
        CommonResponse<?> deleteMediaFile(@RequestParam("url") String url);
}