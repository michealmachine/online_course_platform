package com.double2and9.media.service.impl;

import com.double2and9.base.dto.MediaFileDTO;
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
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.util.DigestUtils;
import org.modelmapper.ModelMapper;
import java.io.File;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MinioClient minioClient;
    private final MediaFileRepository mediaFileRepository;
    private final ModelMapper modelMapper;
    private final FileTypeUtils fileTypeUtils;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public ImageServiceImpl(RedisTemplate<String, Object> redisTemplate,
            MinioClient minioClient,
            MediaFileRepository mediaFileRepository,
            ModelMapper modelMapper,
            FileTypeUtils fileTypeUtils) {
        this.redisTemplate = redisTemplate;
        this.minioClient = minioClient;
        this.mediaFileRepository = mediaFileRepository;
        this.modelMapper = modelMapper;
        this.fileTypeUtils = fileTypeUtils;
    }

    @Override
    /**
     * 上传临时图片到Redis缓存
     * 
     * @param file 上传的图片文件
     * @return 临时存储的key
     * @throws MediaException 文件类型不合法或上传失败时抛出异常
     */
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
    /**
     * 正式上传文件到MinIO存储
     * 
     * @param uploadFileDTO 文件上传DTO（包含文件元数据和内容）
     * @return 保存后的媒体文件实体
     * @throws MediaException 文件上传失败时抛出异常
     */
    public MediaFile uploadFile(UploadFileDTO uploadFileDTO) {
        try {
            // 1. 先检查文件是否已存在
            Optional<MediaFile> existingFile = mediaFileRepository.findByMediaFileId(uploadFileDTO.getFileMd5());
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
                                .build());
            }

            // 4. 创建媒资记录
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileName(uploadFileDTO.getFileName());
            mediaFile.setMediaType(uploadFileDTO.getFileType());
            mediaFile.setFileSize(uploadFileDTO.getFileSize());
            mediaFile.setMediaFileId(uploadFileDTO.getFileMd5());
            mediaFile.setStatus(MediaStatusEnum.UPLOADED.getCode());
            mediaFile.setFilePath(objectName);
            mediaFile.setBucket(bucketName);
            mediaFile.setUrl("/" + bucketName + "/" + objectName); // 设置访问URL

            // 5. 保存到数据库
            return mediaFileRepository.save(mediaFile);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new MediaException(MediaErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    /**
     * 检查文件是否存在
     * 
     * @param fileMd5 文件MD5值
     * @return 存在的媒体文件实体，不存在返回null
     */
    public MediaFile checkFile(String fileMd5) {
        return mediaFileRepository.findByMediaFileId(fileMd5)
                .orElse(null);
    }

    @Override
    /**
     * 删除文件（包含MinIO存储和数据库记录）
     * 
     * @param fileId 文件ID（MD5值）
     * @return 删除成功返回true，文件不存在返回false
     */
    public boolean deleteFile(String fileId) {
        try {
            Optional<MediaFile> mediaFile = mediaFileRepository.findByMediaFileId(fileId);
            if (mediaFile.isEmpty()) {
                return false; // 如果文件不存在，返回false
            }

            // 1. 先从MinIO删除文件
            if (mediaFile.get().getFilePath() != null) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(mediaFile.get().getFilePath())
                                .build());
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
    /**
     * 将临时文件转存到永久存储
     * 
     * @param tempKey 临时文件key
     * @return 文件访问URL
     * @throws MediaException 临时文件不存在或上传失败时抛出异常
     */
    public MediaFile saveTempFile(String tempKey) {
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
                            .build());

            // 4. 删除临时文件
            redisTemplate.delete(tempKey);

            // 5. 创建并返回媒体文件记录
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileName(fileName);
            mediaFile.setMediaType("IMAGE");
            mediaFile.setFileSize(tempFile.getFileSize());
            mediaFile.setFilePath(objectName);
            mediaFile.setBucket(bucketName);
            mediaFile.setUrl("/" + bucketName + "/" + objectName);
            mediaFile.setStatus("NORMAL");
            mediaFile.setCreateTime(new Date());
            mediaFile.setUpdateTime(new Date());

            return mediaFileRepository.save(mediaFile);
        } catch (Exception e) {
            log.error("保存文件到永久存储失败", e);
            throw new MediaException(MediaErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 根据文件名获取MIME类型
     * 
     * @param fileName 文件名
     * @return 对应的MIME类型，默认返回application/octet-stream
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
    /**
     * 更新临时文件内容
     * 
     * @param tempKey 已存在的临时文件key
     * @param file    新的文件内容
     * @return 更新后的临时文件key（与原key相同）
     * @throws MediaException 文件类型不合法或临时文件不存在时抛出异常
     */
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
                            .build());

            redisTemplate.delete(tempKey);
            return "/" + bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("保存文件到永久存储失败", e);
            throw new MediaException(MediaErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    /**
     * 上传课程封面图片
     * 
     * @param organizationId 机构ID，用于权限验证和文件归属
     * @param courseId       课程ID，用于生成唯一文件标识
     * @param file           封面图片文件
     * @return 媒体文件DTO，包含文件访问URL等信息
     * @throws MediaException 当文件类型不支持或上传失败时抛出
     */
    public MediaFileDTO uploadCourseLogo(Long organizationId, Long courseId, MultipartFile file) {
        // 1. 校验文件
        fileTypeUtils.validateImage(file);

        try {
            // 2. 生成文件ID和存储路径
            String mediaFileId = generateMediaFileId(organizationId, courseId, file.getOriginalFilename());
            String filePath = generateFilePath(mediaFileId);

            // 3. 检查是否存在旧文件并删除
            Optional<MediaFile> existingFile = mediaFileRepository.findByMediaFileId(mediaFileId);
            if (existingFile.isPresent()) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(filePath)
                                    .build());
                } catch (Exception e) {
                    log.error("删除旧文件失败：", e);
                    // 继续上传新文件
                }
            }

            // 4. 上传新文件到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            // 5. 生成访问URL
            String url = String.format("/%s/%s", bucketName, filePath);

            // 6. 保存或更新媒体文件记录
            MediaFile mediaFile = existingFile.orElse(new MediaFile());
            mediaFile.setMediaFileId(mediaFileId);
            mediaFile.setOrganizationId(organizationId);
            mediaFile.setFileName(file.getOriginalFilename());
            mediaFile.setMediaType("IMAGE");
            mediaFile.setPurpose("COVER");
            mediaFile.setUrl(url);
            mediaFile.setFileSize(file.getSize());
            mediaFile.setMimeType(file.getContentType());
            mediaFile.setStatus("NORMAL");
            mediaFile.setBucket(bucketName);
            mediaFile.setFilePath(filePath);
            mediaFile.setUpdateTime(new Date());
            if (!existingFile.isPresent()) {
                mediaFile.setCreateTime(new Date());
            }

            mediaFileRepository.save(mediaFile);

            return modelMapper.map(mediaFile, MediaFileDTO.class);
        } catch (Exception e) {
            log.error("上传课程封面失败：", e);
            throw new MediaException(MediaErrorCode.UPLOAD_ERROR, e.getMessage());
        }
    }

    @Override
    /**
     * 根据URL删除媒体文件
     * 
     * @param url 文件访问URL
     * @throws MediaException 文件不存在或删除失败时抛出异常
     */
    public void deleteMediaFile(String url) {
        try {
            // 1. 从URL中提取文件路径
            String filePath = url.substring(url.indexOf("/", 1) + 1);

            // 2. 查找媒体文件记录
            Optional<MediaFile> mediaFileOpt = mediaFileRepository.findByUrl(url);
            if (mediaFileOpt.isEmpty()) {
                throw new MediaException(MediaErrorCode.FILE_NOT_EXISTS);
            }
            MediaFile mediaFile = mediaFileOpt.get();

            // 3. 从MinIO中删除文件
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build());

            // 4. 删除数据库记录
            mediaFileRepository.delete(mediaFile);

            log.info("删除媒体文件成功：{}", url);
        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除媒体文件失败：", e);
            throw new MediaException(MediaErrorCode.DELETE_ERROR, e.getMessage());
        }
    }

    /**
     * 生成课程封面文件的唯一ID（机构ID+课程ID+文件名MD5）
     */
    private String generateMediaFileId(Long organizationId, Long courseId, String fileName) {
        // 获取文件名（不含路径）
        String simpleFileName = new File(fileName).getName();
        return String.format("course_%d_%d_%s", organizationId, courseId,
                DigestUtils.md5DigestAsHex(simpleFileName.getBytes()));
    }

    /**
     * 生成MinIO存储路径（固定目录+文件ID）
     */
    private String generateFilePath(String mediaFileId) {
        return String.format("course/logo/%s", mediaFileId);
    }

    /**
     * 校验是否为图片类型文件
     */
    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}