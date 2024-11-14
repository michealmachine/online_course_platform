package com.double2and9.media.service;

import com.double2and9.base.enums.MediaStatusEnum;
import com.double2and9.media.common.exception.MediaException;
import com.double2and9.media.dto.TempFileDTO;
import com.double2and9.media.dto.UploadFileDTO;
import com.double2and9.media.entity.MediaFile;
import com.double2and9.base.enums.MediaTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class ImageServiceTest {

    @Autowired
    private ImageService imageService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_FILE_PATH = "src/test/resources/test.txt";

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
        assertEquals(mediaFile.getFileId(), duplicateFile.getFileId());
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
        assertEquals(uploadedFile.getId(), existingFile.getId());
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

    @AfterEach
    public void cleanup() {
        // 测试完成后清理测试文件
        try {
            byte[] fileContent = Files.readAllBytes(Path.of(TEST_FILE_PATH));
            String fileMd5 = DigestUtils.md5DigestAsHex(fileContent);
            imageService.deleteFile(fileMd5);
        } catch (Exception e) {
            log.warn("清理测试文件失败：", e);
        }
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
        assertEquals(uploadFileDTO.getFileType(), mediaFile.getFileType());
        assertEquals(uploadFileDTO.getFileMd5(), mediaFile.getFileId());
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
            imageContent
        );

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
            imageContent
        );
        String tempKey = imageService.uploadImageTemp(file);

        // 2. 保存到永久存储
        String url = imageService.saveTempFile(tempKey);
        
        // 3. 验证
        Assertions.assertNotNull(url);
        Assertions.assertTrue(url.startsWith("/"));
        
        // 4. 验证临时文件已删除
        Object tempFile = redisTemplate.opsForValue().get(tempKey);
        Assertions.assertNull(tempFile);
    }

    @Test
    public void testUploadInvalidImage() {
        // 1. 准备非图片文件
        MockMultipartFile file = new MockMultipartFile(
            "test.txt",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        // 2. 验证上传失败
        Assertions.assertThrows(MediaException.class, () -> {
            imageService.uploadImageTemp(file);
        });
    }

    @Test
    public void testUpdateTemp() throws IOException {
        // 1. 先上传一个临时文件
        byte[] originalContent = Files.readAllBytes(Path.of("src/test/resources/test.jpg"));
        MockMultipartFile originalFile = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            originalContent
        );
        String tempKey = imageService.uploadImageTemp(originalFile);

        // 2. 准备新的图片文件
        byte[] newContent = Files.readAllBytes(Path.of("src/test/resources/new_test.jpg"));
        MockMultipartFile newFile = new MockMultipartFile(
            "file",
            "new_test.jpg",
            "image/jpeg",
            newContent
        );

        // 3. 更新临时文件
        String updatedKey = imageService.updateTemp(tempKey, newFile);
        
        // 4. 验证
        // 4.1 验证返回的key没变
        assertEquals(tempKey, updatedKey);
        
        // 4.2 验证Redis中的文件已更新
        Object tempFile = redisTemplate.opsForValue().get(tempKey);
        assertNotNull(tempFile);
        assertTrue(tempFile instanceof TempFileDTO);
        TempFileDTO tempFileDTO = (TempFileDTO) tempFile;
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
            imageContent
        );

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
            imageContent
        );
        String tempKey = imageService.uploadImageTemp(originalFile);

        // 2. 准备无效的文件(非图片)
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

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
} 