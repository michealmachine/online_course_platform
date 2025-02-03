package com.double2and9.media.service;

import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.UploadFileDTO;
import com.double2and9.media.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒资文件服务
 */
public interface ImageService {
    /**
     * 上传文件
     * 
     * @param uploadFileDTO 上传文件信息
     * @return 媒资文件信息
     */
    MediaFile uploadFile(UploadFileDTO uploadFileDTO);

    /**
     * 检查文件是否存在
     * 
     * @param fileMd5 文件MD5
     * @return 媒资文件信息，不存在则返回null
     */
    MediaFile checkFile(String fileMd5);

    /**
     * 删除文件
     * 
     * @param fileId 文件ID（MD5）
     * @return 是否删除成功
     */
    boolean deleteFile(String fileId);

    /**
     * 上传图片到临时存储
     * 
     * @param file 图片文件
     * @return 临时存储的key
     */
    String uploadImageTemp(MultipartFile file);

    /**
     * 更新临时存储的图片
     */
    String updateTemp(String tempKey, MultipartFile file);

    /**
     * 将临时文件保存到永久存储
     * 
     * @param tempKey 临时存储key
     * @return 永久访问URL
     */
    String saveTempFile(String tempKey);

    /**
     * 保存临时文件到永久存储（saveTempFile的别名方法）
     *
     * @param tempKey 临时文件key
     * @return 文件访问URL
     * @throws MediaException 临时文件不存在或上传失败时抛出异常
     */
    String saveTemp(String tempKey);

    /**
     * 上传课程封面图片
     * 
     * @param organizationId 机构ID
     * @param courseId       课程ID
     * @param file           图片文件
     * @return 媒体文件信息
     */
    MediaFileDTO uploadCourseLogo(Long organizationId, Long courseId, MultipartFile file);

    /**
     * 删除媒体文件
     * 
     * @param url 文件URL
     */
    void deleteMediaFile(String url);

}