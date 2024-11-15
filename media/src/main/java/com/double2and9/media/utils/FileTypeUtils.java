package com.double2and9.media.utils;

import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

public class FileTypeUtils {
    
    // 允许的图片格式
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg", 
        "image/png",
        "image/gif"
    );
    
    // 允许的图片后缀
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
        ".jpg",
        ".jpeg",
        ".png",
        ".gif"
    );
    
    /**
     * 校验图片格式
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
} 