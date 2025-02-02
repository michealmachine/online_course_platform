package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.MediaFile;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.entity.TeachplanMedia;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
public class TeachplanMediaRepositoryTest {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private TeachplanMediaRepository teachplanMediaRepository;
    
    @Autowired
    private TeachplanRepository teachplanRepository;
    
    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Test
    void testSaveTeachplanMedia() {
        // 准备测试数据
        Teachplan teachplan = new Teachplan();
        teachplan.setName("测试章节");
        teachplan = teachplanRepository.save(teachplan);

        // 创建MediaFile
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaFileId("test123");  // 设置mediaFileId作为主键
        mediaFile.setOrganizationId(TEST_ORG_ID);
        mediaFile.setFileName("测试视频");
        mediaFile.setMediaType("VIDEO");
        mediaFile = mediaFileRepository.save(mediaFile);

        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setTeachplan(teachplan);
        teachplanMedia.setMediaFile(mediaFile);
        teachplanMedia.setCreateTime(new Date());
        teachplanMedia.setUpdateTime(new Date());

        // 执行保存
        TeachplanMedia saved = teachplanMediaRepository.save(teachplanMedia);

        // 验证
        assertNotNull(saved.getId());
        assertEquals(teachplan.getId(), saved.getTeachplan().getId());
        assertEquals(mediaFile.getMediaFileId(), saved.getMediaFile().getMediaFileId());
    }

    @Test
    void testFindByTeachplanId() {
        // 准备测试数据
        Teachplan teachplan = new Teachplan();
        teachplan.setName("测试章节");
        teachplan = teachplanRepository.save(teachplan);

        // 创建MediaFile
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaFileId("test456");  // 设置不同的mediaFileId
        mediaFile.setOrganizationId(TEST_ORG_ID);
        mediaFile.setFileName("测试视频");
        mediaFile.setMediaType("VIDEO");
        mediaFile = mediaFileRepository.save(mediaFile);

        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setTeachplan(teachplan);
        teachplanMedia.setMediaFile(mediaFile);
        teachplanMedia.setCreateTime(new Date());
        teachplanMedia.setUpdateTime(new Date());
        teachplanMediaRepository.save(teachplanMedia);

        // 执行查询
        List<TeachplanMedia> result = teachplanMediaRepository.findByTeachplanId(teachplan.getId());

        // 验证
        assertFalse(result.isEmpty());
        assertEquals(teachplan.getId(), result.get(0).getTeachplan().getId());
        assertEquals(mediaFile.getMediaFileId(), result.get(0).getMediaFile().getMediaFileId());
    }

    @Test
    void testFindByTeachplanIdAndMediaFileId() {
        // 准备测试数据
        Teachplan teachplan = new Teachplan();
        teachplan.setName("测试章节");
        teachplan = teachplanRepository.save(teachplan);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaFileId("test789");
        mediaFile.setOrganizationId(TEST_ORG_ID);
        mediaFile.setFileName("测试视频");
        mediaFile.setMediaType("VIDEO");
        mediaFile = mediaFileRepository.save(mediaFile);

        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setTeachplan(teachplan);
        teachplanMedia.setMediaFile(mediaFile);
        teachplanMedia.setCreateTime(new Date());
        teachplanMedia.setUpdateTime(new Date());
        teachplanMediaRepository.save(teachplanMedia);

        // 执行查询
        Optional<TeachplanMedia> result = teachplanMediaRepository
            .findByTeachplanIdAndMediaFile_MediaFileId(teachplan.getId(), "test789");

        // 验证
        assertTrue(result.isPresent());
        assertEquals(teachplan.getId(), result.get().getTeachplan().getId());
        assertEquals("test789", result.get().getMediaFile().getMediaFileId());
    }
} 