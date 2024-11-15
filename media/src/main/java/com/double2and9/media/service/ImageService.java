package com.double2and9.media.service;

import com.double2and9.media.dto.UploadFileDTO;
import com.double2and9.media.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒资文件服务
 */
public interface ImageService {
    /**
     * 上传文件
     * @param uploadFileDTO 上传文件信息
     * @return 媒资文件信息
     */
    MediaFile uploadFile(UploadFileDTO uploadFileDTO);

    /**
     * 检查文件是否存在
     * @param fileMd5 文件MD5
     * @return 媒资文件信息，不存在则返回null
     */
    MediaFile checkFile(String fileMd5);

    /**
     * 删除文件
     * @param fileId 文件ID（MD5）
     * @return 是否删除成功
     */
    boolean deleteFile(String fileId);

    /**
     * 上传图片到临时存储
     * @param file 图片文件
     * @return 临时存储的key
     */
    String uploadImageTemp(MultipartFile file);

    /**
     * 将临时文件保存到永久存储
     * @param tempKey 临时存储key
     * @return 永久访问URL
     */
    String saveTempFile(String tempKey);

    String updateTemp(String tempKey, MultipartFile file);

    String saveTemp(String tempKey);
}