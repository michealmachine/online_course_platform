package com.double2and9.content_service.service;

import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.entity.MediaFile;
import com.double2and9.content_service.repository.MediaFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MediaFileServiceTests {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Test
    void testSaveMediaFile() {
        // 准备测试数据
        MediaFileDTO dto = new MediaFileDTO();
        dto.setMediaFileId("test123");
        dto.setFileName("test.jpg");
        dto.setMediaType("IMAGE");
        dto.setFileSize(1024L);
        dto.setMimeType("image/jpeg");
        dto.setPurpose("COVER");
        dto.setUrl("http://example.com/test.jpg");

        // 执行保存
        MediaFile saved = mediaFileService.saveMediaFile(TEST_ORG_ID, dto);

        // 验证结果
        assertNotNull(saved);
        assertEquals("test123", saved.getMediaFileId());
        assertEquals(TEST_ORG_ID, saved.getOrganizationId());
        assertEquals("202003", saved.getAuditStatus()); // 图片默认通过
    }

    @Test
    void testSaveVideoMediaFile() {
        // 准备测试数据
        MediaFileDTO dto = new MediaFileDTO();
        dto.setMediaFileId("test456");
        dto.setFileName("test.mp4");
        dto.setMediaType("VIDEO");
        dto.setFileSize(1024L);
        dto.setMimeType("video/mp4");
        dto.setPurpose("VIDEO");
        dto.setUrl("http://example.com/test.mp4");

        // 执行保存
        MediaFile saved = mediaFileService.saveMediaFile(TEST_ORG_ID, dto);

        // 验证结果
        assertNotNull(saved);
        assertEquals("test456", saved.getMediaFileId());
        assertEquals("202001", saved.getAuditStatus()); // 视频默认待审核
    }

    @Test
    void testQueryMediaFiles() {
        // 准备测试数据 - 图片
        MediaFileDTO imageDto = new MediaFileDTO();
        imageDto.setMediaFileId("test123");
        imageDto.setFileName("test.jpg");
        imageDto.setMediaType("IMAGE");
        imageDto.setPurpose("COVER");
        MediaFile savedImage = mediaFileService.saveMediaFile(TEST_ORG_ID, imageDto);
        assertEquals("test123", savedImage.getMediaFileId());

        // 准备测试数据 - 视频
        MediaFileDTO videoDto = new MediaFileDTO();
        videoDto.setMediaFileId("test456");
        videoDto.setFileName("test.mp4");
        videoDto.setMediaType("VIDEO");
        videoDto.setPurpose("VIDEO");
        MediaFile savedVideo = mediaFileService.saveMediaFile(TEST_ORG_ID, videoDto);
        assertEquals("test456", savedVideo.getMediaFileId());

        // 测试分页查询
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(10L);

        // 查询所有媒体文件
        PageResult<MediaFileDTO> result = mediaFileService.queryMediaFiles(
                TEST_ORG_ID, null, null, pageParams);
        assertEquals(2, result.getItems().size());

        // 按媒体类型查询
        result = mediaFileService.queryMediaFiles(
                TEST_ORG_ID, "IMAGE", null, pageParams);
        assertEquals(1, result.getItems().size());
        assertEquals("IMAGE", result.getItems().get(0).getMediaType());

        // 按用途查询
        result = mediaFileService.queryMediaFiles(
                TEST_ORG_ID, null, "VIDEO", pageParams);
        assertEquals(1, result.getItems().size());
        assertEquals("VIDEO", result.getItems().get(0).getPurpose());
    }

    @Test
    void testGetMediaFileUrl() {
        // 准备图片测试数据
        MediaFileDTO imageDto = new MediaFileDTO();
        imageDto.setMediaFileId("test123");
        imageDto.setFileName("test.jpg");
        imageDto.setMediaType("IMAGE");
        imageDto.setUrl("http://example.com/test.jpg");
        mediaFileService.saveMediaFile(TEST_ORG_ID, imageDto);

        // 测试获取图片URL
        String url = mediaFileService.getMediaFileUrl(TEST_ORG_ID, "test123");
        assertEquals("http://example.com/test.jpg", url);

        // 准备视频测试数据
        MediaFileDTO videoDto = new MediaFileDTO();
        videoDto.setMediaFileId("test456");
        videoDto.setFileName("test.mp4");
        videoDto.setMediaType("VIDEO");
        mediaFileService.saveMediaFile(TEST_ORG_ID, videoDto);

        // 测试获取视频URL（应该抛出异常）
        assertThrows(ContentException.class, () -> mediaFileService.getMediaFileUrl(TEST_ORG_ID, "test456"));
    }

    @Test
    void testUpdateAuditStatus() {
        // 准备测试数据
        MediaFileDTO dto = new MediaFileDTO();
        dto.setMediaFileId("test123");
        dto.setFileName("test.mp4");
        dto.setMediaType("VIDEO");
        MediaFile saved = mediaFileService.saveMediaFile(TEST_ORG_ID, dto);
        assertEquals("202001", saved.getAuditStatus()); // 初始状态：待审核

        // 更新审核状态
        mediaFileService.updateAuditStatus(
                "test123", "202003", "审核通过");

        // 验证更新结果
        Optional<MediaFile> updated = mediaFileRepository.findByMediaFileId("test123");
        assertTrue(updated.isPresent());
        assertEquals("202003", updated.get().getAuditStatus());
        assertEquals("审核通过", updated.get().getAuditMessage());
    }

    @Test
    void testGetMediaFileUrlWithWrongOrg() {
        // 准备测试数据
        MediaFileDTO dto = new MediaFileDTO();
        dto.setMediaFileId("test123");
        dto.setFileName("test.jpg");
        dto.setMediaType("IMAGE");
        mediaFileService.saveMediaFile(TEST_ORG_ID, dto);

        // 使用错误的机构ID访问
        Long wrongOrgId = 9999L;
        assertThrows(ContentException.class, () -> mediaFileService.getMediaFileUrl(wrongOrgId, "test123"));
    }
}