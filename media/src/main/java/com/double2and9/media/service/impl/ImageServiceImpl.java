package com.double2and9.media.service.impl;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.base.enums.MediaStatusEnum;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.TempFileDTO;
import com.double2and9.media.dto.UploadFileDTO;
import com.double2and9.media.entity.MediaFile;
import com.double2and9.media.repository.MediaFileRepository;
import com.double2and9.media.service.ImageService;
import com.double2and9.media.utils.FileTypeUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MinioClient minioClient;
    private final MediaFileRepository mediaFileRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public ImageServiceImpl(RedisTemplate<String, Object> redisTemplate,
                            MinioClient minioClient,
                            MediaFileRepository mediaFileRepository) {
        this.redisTemplate = redisTemplate;
        this.minioClient = minioClient;
        this.mediaFileRepository = mediaFileRepository;
    }

    @Override
    public String uploadImageTemp(MultipartFile file) {
        // 1. 校验文件
        if (!FileTypeUtils.isAllowedImage(file)) {
            throw new MediaException(MediaErrorCode.FILE_TYPE_ERROR);
        }

        try {
            // 2. 生成临时存储key
            String tempKey = "media:temp:image:" + UUID.randomUUID();

            // 3. 构建临时文件对象
            TempFileDTO tempFile = new TempFileDTO();
            tempFile.setFileName(file.getOriginalFilename());
            tempFile.setContentType(file.getContentType());
            tempFile.setFileData(file.getBytes());
            tempFile.setFileSize(file.getSize());

            // 4. 存入Redis,30分钟过期
            redisTemplate.opsForValue().set(tempKey, tempFile, 30, TimeUnit.MINUTES);

            return tempKey;
        } catch (Exception e) {
            log.error("上传图片到临时存储失败", e);
            throw new MediaException(MediaErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public MediaFile uploadFile(UploadFileDTO uploadFileDTO) {
        try {
            // 1. 先检查文件是否已存在
            Optional<MediaFile> existingFile = mediaFileRepository.findByFileId(uploadFileDTO.getFileMd5());
            if (existingFile.isPresent()) {
                return existingFile.get();
            }

            // 2. 生成文件存储路径
            String extension = uploadFileDTO.getFileName().substring(
                uploadFileDTO.getFileName().lastIndexOf("."));
            String objectName = "files/" + uploadFileDTO.getFileMd5() + extension;

            // 3. 上传到MinIO
            if (uploadFileDTO.getFileData() != null) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(new ByteArrayInputStream(uploadFileDTO.getFileData()), 
                               uploadFileDTO.getFileData().length, -1)
                        .contentType(getContentType(uploadFileDTO.getFileName()))
                        .build()
                );
            }

            // 4. 创建媒资记录
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileName(uploadFileDTO.getFileName());
            mediaFile.setFileType(uploadFileDTO.getFileType());
            mediaFile.setFileSize(uploadFileDTO.getFileSize());
            mediaFile.setFileMd5(uploadFileDTO.getFileMd5());
            mediaFile.setFileId(uploadFileDTO.getFileMd5());
            mediaFile.setStatus(MediaStatusEnum.UPLOADED.getCode());
            mediaFile.setFilePath(objectName);
            mediaFile.setBucket(bucketName);
            mediaFile.setUrl("/" + bucketName + "/" + objectName);  // 设置访问URL
            
            // 5. 保存到数据库
            return mediaFileRepository.save(mediaFile);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new MediaException(MediaErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public MediaFile checkFile(String fileMd5) {
        return mediaFileRepository.findByFileId(fileMd5)
                .orElse(null);
    }

    @Override
    public boolean deleteFile(String fileId) {
        try {
            Optional<MediaFile> mediaFile = mediaFileRepository.findByFileId(fileId);
            if (mediaFile.isEmpty()) {
                return false;  // 如果文件不存在，返回false
            }

            // 1. 先从MinIO删除文件
            if (mediaFile.get().getFilePath() != null) {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(mediaFile.get().getFilePath())
                        .build()
                );
            }

            // 2. 再从数据库删除记录
            mediaFileRepository.delete(mediaFile.get());
            return true;
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return false;
        }
    }

    @Override
    public String saveTempFile(String tempKey) {
        // 1. 从Redis获取临时文件
        TempFileDTO tempFile = (TempFileDTO) redisTemplate.opsForValue().get(tempKey);
        if (tempFile == null) {
            throw new MediaException(MediaErrorCode.FILE_NOT_EXISTS);
        }

        try {
            // 2. 生成MinIO存储路径
            String fileName = tempFile.getFileName();
            String extension = fileName.substring(fileName.lastIndexOf("."));
            String objectName = "images/" + UUID.randomUUID() + extension;

            // 3. 上传到MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(tempFile.getFileData()), 
                           tempFile.getFileData().length, -1)
                    .contentType(tempFile.getContentType())
                    .build()
            );

            // 4. 删除临时文件
            redisTemplate.delete(tempKey);

            // 5. 返回访问URL - 使用简单的URL拼接
            return "/" + bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("保存文件到永久存储失败", e);
            throw new MediaException(MediaErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 根据文件名获取contentType
     */
    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    @Override
    public String updateTemp(String tempKey, MultipartFile file) {
        // 1. 验证文件格式
        if (!FileTypeUtils.isAllowedImage(file)) {
            throw new MediaException(MediaErrorCode.FILE_TYPE_ERROR);
        }

        try {
            // 2. 检查临时文件是否存在
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(tempKey))) {
                throw new MediaException(MediaErrorCode.FILE_NOT_EXISTS);
            }

            // 3. 更新临时文件
            TempFileDTO tempFile = new TempFileDTO();
            tempFile.setFileName(file.getOriginalFilename());
            tempFile.setContentType(file.getContentType());
            tempFile.setFileData(file.getBytes());
            tempFile.setFileSize(file.getSize());

            // 4. 重置过期时间
            redisTemplate.opsForValue().set(tempKey, tempFile, 30, TimeUnit.MINUTES);
            
            return tempKey;
        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新临时图片失败", e);
            throw new MediaException(MediaErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String saveTemp(String tempKey) {
        // 原有的saveTempFile方法逻辑
        TempFileDTO tempFile = (TempFileDTO) redisTemplate.opsForValue().get(tempKey);
        if (tempFile == null) {
            throw new MediaException(MediaErrorCode.FILE_NOT_EXISTS);
        }

        try {
            String fileName = tempFile.getFileName();
            String extension = fileName.substring(fileName.lastIndexOf("."));
            String objectName = "images/" + UUID.randomUUID() + extension;

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(tempFile.getFileData()), 
                           tempFile.getFileData().length, -1)
                    .contentType(tempFile.getContentType())
                    .build()
            );

            redisTemplate.delete(tempKey);
            return "/" + bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("保存文件到永久存储失败", e);
            throw new MediaException(MediaErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // ... 其他方法的实现
} 