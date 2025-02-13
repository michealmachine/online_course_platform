package com.double2and9.media.service;

import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.base.enums.MediaStatusEnum;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.TempFileDTO;
import com.double2and9.media.dto.UploadFileDTO;
import com.double2and9.media.entity.MediaFile;
import com.double2and9.base.enums.MediaTypeEnum;
import com.double2and9.media.repository.MediaFileRepository;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetObjectArgs;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.springframework.test.util.ReflectionTestUtils;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;

@SpringBootTest
@Slf4j
public class ImageServiceTest {

    @Autowired
    private ImageService imageService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private static final String TEST_FILE_PATH = "src/test/resources/test.txt";
    private static final Long TEST_ORG_ID = 1L;
    private static final Long TEST_COURSE_ID = 1L;

    @BeforeEach
    void setUp() throws Exception {
        // 确保测试用的 bucket 存在
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @AfterEach
    void cleanup() {
        // 清理所有测试数据
        mediaFileRepository.deleteAll();

        // 恢复原始 bucketName（针对 MinIO 错误测试）
        ReflectionTestUtils.setField(imageService, "bucketName", bucketName);
    }

    @Test
    public void testUploadFileWithPath() throws IOException {
        // 1. 准备测试数据
        File testFile = new File(TEST_FILE_PATH);
        byte[] fileContent = Files.readAllBytes(testFile.toPath());
        String fileMd5 = DigestUtils.md5DigestAsHex(fileContent);

        // 2. 构建上传DTO
        UploadFileDTO uploadFileDTO = createUploadDTO(testFile, fileMd5);
        uploadFileDTO.setFilePath(TEST_FILE_PATH);

        // 3. 上传文件
        MediaFile mediaFile = imageService.uploadFile(uploadFileDTO);

        // 4. 验证结果
        assertUploadResult(mediaFile, uploadFileDTO);

        // 5. 测试文件去重 - 应该返回相同的记录
        MediaFile duplicateFile = imageService.uploadFile(uploadFileDTO);
        assertEquals(mediaFile.getId(), duplicateFile.getId());
    }

    @Test
    public void testUploadFileWithBytes() throws IOException {
        // 1. 准备测试数据
        byte[] fileContent = Files.readAllBytes(Path.of(TEST_FILE_PATH));
        String fileMd5 = DigestUtils.md5DigestAsHex(fileContent);

        // 2. 建上传DTO
        UploadFileDTO uploadFileDTO = createUploadDTO(new File(TEST_FILE_PATH), fileMd5);
        uploadFileDTO.setFileData(fileContent);

        // 3. 上传文件
        MediaFile mediaFile = imageService.uploadFile(uploadFileDTO);

        // 4. 验证结果
        assertUploadResult(mediaFile, uploadFileDTO);
    }

    @Test
    public void testCheckFile() throws IOException {
        // 1. 准备数据
        byte[] fileContent = Files.readAllBytes(Path.of(TEST_FILE_PATH));
        String fileMd5 = DigestUtils.md5DigestAsHex(fileContent);

        // 2. 检查不存在的文件
        assertNull(imageService.checkFile("not_exist_md5"));

        // 3. 上传文件
        UploadFileDTO uploadFileDTO = createUploadDTO(new File(TEST_FILE_PATH), fileMd5);
        uploadFileDTO.setFilePath(TEST_FILE_PATH);
        MediaFile uploadedFile = imageService.uploadFile(uploadFileDTO);

        // 4. 检查已存在的文件
        MediaFile existingFile = imageService.checkFile(fileMd5);
        assertNotNull(existingFile);
        assertEquals(uploadedFile.getMediaFileId(), existingFile.getMediaFileId());
        assertEquals(uploadedFile.getMediaFileId(), existingFile.getMediaFileId());
    }

    @Test
    public void testDeleteFile() throws IOException {
        // 1. 先上传一个文件
        byte[] fileContent = Files.readAllBytes(Path.of(TEST_FILE_PATH));
        String fileMd5 = DigestUtils.md5DigestAsHex(fileContent);

        UploadFileDTO uploadFileDTO = createUploadDTO(new File(TEST_FILE_PATH), fileMd5);
        uploadFileDTO.setFilePath(TEST_FILE_PATH);
        MediaFile uploadedFile = imageService.uploadFile(uploadFileDTO);

        // 2. 验证文件存在
        assertNotNull(imageService.checkFile(fileMd5));

        // 3. 删除文件
        assertTrue(imageService.deleteFile(fileMd5));

        // 4. 验证文件已删除
        assertNull(imageService.checkFile(fileMd5));

        // 5. 删除不存在的文件
        assertFalse(imageService.deleteFile("not_exist_md5"));
    }

    /**
     * 创建上传DTO
     */
    private UploadFileDTO createUploadDTO(File file, String fileMd5) {
        UploadFileDTO dto = new UploadFileDTO();
        dto.setFileName(file.getName());
        dto.setFileType(MediaTypeEnum.DOC.getCode());
        dto.setFileMd5(fileMd5);
        dto.setFileSize(file.length());
        dto.setUsername("test");
        return dto;
    }

    /**
     * 验证上传结果
     */
    private void assertUploadResult(MediaFile mediaFile, UploadFileDTO uploadFileDTO) {
        assertNotNull(mediaFile);
        assertEquals(uploadFileDTO.getFileName(), mediaFile.getFileName());
        assertEquals(uploadFileDTO.getFileType(), mediaFile.getMediaType());
        assertEquals(uploadFileDTO.getFileMd5(), mediaFile.getMediaFileId());
        assertEquals(uploadFileDTO.getFileSize(), mediaFile.getFileSize());
        assertEquals(MediaStatusEnum.UPLOADED.getCode(), mediaFile.getStatus());
    }

    @Test
    public void testUploadImageTemp() throws IOException {
        // 1. 准备测试图片 - 使用真实的图片文件
        byte[] imageContent = Files.readAllBytes(Path.of("src/test/resources/test.jpg"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageContent);

        // 2. 上传到临时存储
        String tempKey = imageService.uploadImageTemp(file);

        // 3. 验证
        Assertions.assertNotNull(tempKey);
        Assertions.assertTrue(tempKey.startsWith("media:temp:image:"));

        // 4. 验证Redis中存在数据
        Object tempFile = redisTemplate.opsForValue().get(tempKey);
        Assertions.assertNotNull(tempFile);
    }

    @Test
    public void testSaveTempFile() throws IOException {
        // 1. 先上传临时文件
        byte[] imageContent = Files.readAllBytes(Path.of("src/test/resources/test.jpg"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageContent);
        String tempKey = imageService.uploadImageTemp(file);

        // 2. 保存到永久存储
        MediaFile mediaFile = imageService.saveTempFile(tempKey);

        // 3. 验证
        assertNotNull(mediaFile);
        assertEquals("test.jpg", mediaFile.getFileName());
        assertEquals("IMAGE", mediaFile.getMediaType());
        assertTrue(mediaFile.getUrl().startsWith("/"));
        assertEquals("NORMAL", mediaFile.getStatus());
        assertNotNull(mediaFile.getCreateTime());
        assertNotNull(mediaFile.getUpdateTime());

        // 4. 验证临时文件已删除
        Object tempFile = redisTemplate.opsForValue().get(tempKey);
        assertNull(tempFile);
    }

    @Test
    public void testUpdateTemp() throws IOException {
        // 1. 先上传一个临时文件
        byte[] originalContent = Files.readAllBytes(Path.of("src/test/resources/test.jpg"));
        MockMultipartFile originalFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                originalContent);
        String tempKey = imageService.uploadImageTemp(originalFile);

        // 2. 准备新的图片文件
        byte[] newContent = Files.readAllBytes(Path.of("src/test/resources/new_test.jpg"));
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new_test.jpg",
                "image/jpeg",
                newContent);

        // 3. 更新临时文件
        String updatedKey = imageService.updateTemp(tempKey, newFile);

        // 4. 验证
        assertEquals(tempKey, updatedKey);
        TempFileDTO tempFileDTO = (TempFileDTO) redisTemplate.opsForValue().get(tempKey);
        assertNotNull(tempFileDTO);
        assertEquals("new_test.jpg", tempFileDTO.getFileName());
        assertArrayEquals(newContent, tempFileDTO.getFileData());
    }

    @Test
    public void testUpdateTempWithInvalidKey() throws IOException {
        // 1. 准备测试文件
        byte[] imageContent = Files.readAllBytes(Path.of("src/test/resources/test.jpg"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageContent);

        // 2. 使用不存在的key更新
        String invalidKey = "media:temp:image:not-exists";

        // 3. 验证抛出异常
        assertThrows(MediaException.class, () -> {
            imageService.updateTemp(invalidKey, file);
        });
    }

    @Test
    public void testUpdateTempWithInvalidFile() throws IOException {
        // 1. 先上传一个有效的临时文件
        byte[] imageContent = Files.readAllBytes(Path.of("src/test/resources/test.jpg"));
        MockMultipartFile originalFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageContent);
        String tempKey = imageService.uploadImageTemp(originalFile);

        // 2. 准备无效的文件(非图片)
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes());

        // 3. 验证更新失败
        assertThrows(MediaException.class, () -> {
            imageService.updateTemp(tempKey, invalidFile);
        });

        // 4. 验证原文件未被修改
        Object tempFile = redisTemplate.opsForValue().get(tempKey);
        assertNotNull(tempFile);
        assertTrue(tempFile instanceof TempFileDTO);
        TempFileDTO tempFileDTO = (TempFileDTO) tempFile;
        assertEquals("test.jpg", tempFileDTO.getFileName());
        assertArrayEquals(imageContent, tempFileDTO.getFileData());
    }

    @Test
    @Transactional
    void testUploadCourseLogo() throws IOException, ErrorResponseException, InsufficientDataException,
            InternalException, InvalidKeyException, InvalidResponseException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        // 1. 准备测试数据 - 使用合适大小的图片
        byte[] imageContent = new byte[500 * 1024]; // 500KB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageContent);

        // 2. 上传文件
        MediaFileDTO result = imageService.uploadCourseLogo(TEST_ORG_ID, TEST_COURSE_ID, file);

        // 3. 验证结果
        assertNotNull(result);
        assertEquals(TEST_ORG_ID, result.getOrganizationId());
        assertEquals("IMAGE", result.getMediaType());
        assertEquals("COVER", result.getPurpose());
        assertEquals("image/jpeg", result.getMimeType());
        assertEquals("NORMAL", result.getStatus());

        // 4. 验证数据库记录
        Optional<MediaFile> savedFileOpt = mediaFileRepository.findByMediaFileId(result.getMediaFileId());
        assertTrue(savedFileOpt.isPresent(), "Saved file should exist");
        MediaFile savedFile = savedFileOpt.get();
        assertNotNull(savedFile);
        assertEquals(file.getSize(), savedFile.getFileSize());
        assertEquals(bucketName, savedFile.getBucket());

        // 5. 验证MinIO中的文件
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(savedFile.getUrl().substring(savedFile.getUrl().indexOf("/", 1) + 1))
                            .build());
        } catch (Exception e) {
            fail("File should exist in MinIO");
        }
    }

    @Test
    @Transactional
    void testUploadCourseLogo_InvalidFileType() {
        // 1. 准备无效文件类型
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes());

        // 2. 验证异常
        MediaException exception = assertThrows(MediaException.class,
                () -> imageService.uploadCourseLogo(TEST_ORG_ID, TEST_COURSE_ID, file));
        assertEquals(MediaErrorCode.MEDIA_TYPE_NOT_SUPPORT, exception.getErrorCode());
    }

    @Test
    @Transactional
    void testUpdateCourseLogo() throws IOException, ErrorResponseException, InsufficientDataException,
            InternalException, InvalidKeyException, InvalidResponseException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        // 1. 先上传一个文件
        byte[] imageContent = new byte[500 * 1024]; // 500KB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageContent);
        MediaFileDTO originalFile = imageService.uploadCourseLogo(TEST_ORG_ID, TEST_COURSE_ID, file);

        // 2. 准备新文件（使用相同的文件名，但内容不同）
        byte[] newImageContent = new byte[600 * 1024]; // 600KB
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                newImageContent);

        // 3. 使用uploadCourseLogo更新文件
        MediaFileDTO updatedFile = imageService.uploadCourseLogo(TEST_ORG_ID, TEST_COURSE_ID, newFile);

        // 4. 验证结果
        assertEquals(newImageContent.length, updatedFile.getFileSize());
        assertNotEquals(imageContent.length, updatedFile.getFileSize());

        // 5. 验证MinIO中的文件内容已更新
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(updatedFile.getUrl().substring(updatedFile.getUrl().indexOf("/", 1) + 1))
                        .build())) {
            byte[] storedContent = response.readAllBytes();
            assertArrayEquals(newImageContent, storedContent);
        }
    }

    @Test
    @Transactional
    void testDeleteCourseLogo() throws IOException, ErrorResponseException, InsufficientDataException,
            InternalException, InvalidKeyException, InvalidResponseException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        // 1. 先上传一个文件
        byte[] imageContent = new byte[500 * 1024]; // 500KB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageContent);
        MediaFileDTO uploadedFile = imageService.uploadCourseLogo(TEST_ORG_ID, TEST_COURSE_ID, file);

        // 2. 删除文件
        imageService.deleteMediaFile(uploadedFile.getUrl());

        // 3. 验证数据库记录已删除
        Optional<MediaFile> deletedFileOpt = mediaFileRepository.findByMediaFileId(uploadedFile.getMediaFileId());
        assertFalse(deletedFileOpt.isPresent(), "File should be deleted");

        // 4. 验证MinIO中的文件已删除
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uploadedFile.getUrl().substring(uploadedFile.getUrl().indexOf("/", 1) + 1))
                            .build());
            fail("Expected file to be deleted");
        } catch (Exception e) {
            // 文件已被删除，抛出异常是预期行为
        }
    }

    @Test
    @Transactional
    void testDeleteCourseLogo_NotFound() {
        // 1. 尝试删除不存在的文件
        String nonExistentUrl = "/media/non-existent.jpg";

        // 2. 验证异常
        try {
            imageService.deleteMediaFile(nonExistentUrl);
            fail("Expected MediaException to be thrown");
        } catch (MediaException e) {
            assertEquals(MediaErrorCode.FILE_NOT_EXISTS.getCode(), e.getCode());
            assertEquals(MediaErrorCode.FILE_NOT_EXISTS.getMessage(), e.getMessage());
        }
    }

    // 添加辅助方法用于生成mediaFileId
    private String generateMediaFileId(Long organizationId, Long courseId, String fileName) {
        return String.format("course_%d_%d_%s", organizationId, courseId,
                DigestUtils.md5DigestAsHex(fileName.getBytes()));
    }

    // 添加新的错误处理测试用例
    @Test
    void testUploadCourseLogo_MinioError() {
        // 准备测试数据 - 使用合法大小的文件
        byte[] content = new byte[10 * 1024]; // 10KB，确保通过大小校验
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", content);

        // 修改bucketName为不存在的bucket以触发MinIO错误
        ReflectionTestUtils.setField(imageService, "bucketName", "non-existent-bucket");

        // 验证异常
        MediaException exception = assertThrows(MediaException.class,
                () -> imageService.uploadCourseLogo(TEST_ORG_ID, TEST_COURSE_ID, file));
        assertEquals(MediaErrorCode.UPLOAD_ERROR, exception.getErrorCode());
    }

    @Test
    void testUploadCourseLogo_EmptyFile() {
        // 1. 准备空文件
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        // 2. 验证上传空文件时抛出异常
        MediaException exception = assertThrows(MediaException.class,
                () -> imageService.uploadCourseLogo(TEST_ORG_ID, TEST_COURSE_ID, emptyFile));
        assertEquals(MediaErrorCode.FILE_EMPTY, exception.getErrorCode());
    }

    @Test
    void testUploadCourseLogo_FileTooLarge() {
        // 准备超大文件
        byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", largeContent);

        // 验证异常
        MediaException exception = assertThrows(MediaException.class,
                () -> imageService.uploadCourseLogo(1L, 1L, file));
        assertEquals(MediaErrorCode.FILE_TOO_LARGE, exception.getErrorCode());
    }

    @Test
    void testUploadCourseLogo_FileTooSmall() {
        // 准备过小文件
        byte[] smallContent = new byte[100]; // 100B
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", smallContent);

        // 验证异常
        MediaException exception = assertThrows(MediaException.class,
                () -> imageService.uploadCourseLogo(1L, 1L, file));
        assertEquals(MediaErrorCode.FILE_TOO_SMALL, exception.getErrorCode());
    }

    @Test
    void testUploadInvalidImage() {
        // 准备非图片文件
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test".getBytes());

        // 验证异常
        MediaException exception = assertThrows(MediaException.class,
                () -> imageService.uploadCourseLogo(1L, 1L, file));
        assertEquals(MediaErrorCode.MEDIA_TYPE_NOT_SUPPORT, exception.getErrorCode());
    }
}