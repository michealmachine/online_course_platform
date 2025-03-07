package com.double2and9.media.utils;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.media.common.exception.MediaException;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "media.image")
@Data
public class FileTypeUtils {

    // 允许的图片格式
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif");

    // 允许的图片后缀
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif");

    // 使用 ALLOWED_IMAGE_TYPES 作为默认值
    private List<String> allowedTypes = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif");

    private long maxSize = 2 * 1024 * 1024; // 默认2MB
    private long minSize = 1024; // 默认1KB

    /**
     * 校验图片格式
     * 
     * @param file 上传的文件
     * @return 是否是允许的图片格式
     */
    public static boolean isAllowedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // 检查Content-Type
        String contentType = file.getContentType();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return false;
        }

        // 检查文件后缀
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        return ALLOWED_IMAGE_EXTENSIONS.stream()
                .anyMatch(ext -> originalFilename.toLowerCase().endsWith(ext));
    }

    public void validateImage(MultipartFile file) {
        // 1. 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new MediaException(MediaErrorCode.FILE_EMPTY);
        }

        // 2. 检查文件类型
        String contentType = file.getContentType();
        if (!allowedTypes.contains(contentType)) {
            throw new MediaException(MediaErrorCode.MEDIA_TYPE_NOT_SUPPORT);
        }

        // 3. 检查文件大小
        long size = file.getSize();
        if (size > maxSize) {
            throw new MediaException(MediaErrorCode.FILE_TOO_LARGE,
                    String.format("文件大小超过限制: %d > %d", size, maxSize));
        }
        if (size < minSize) {
            throw new MediaException(MediaErrorCode.FILE_TOO_SMALL,
                    String.format("文件大小过小: %d < %d", size, minSize));
        }
    }
}