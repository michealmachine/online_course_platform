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

        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName("测试视频");
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
        assertEquals(mediaFile.getId(), saved.getMediaFile().getId());
    }

    @Test
    void testFindByTeachplanId() {
        // 准备测试数据
        Teachplan teachplan = new Teachplan();
        teachplan.setName("测试章节");
        teachplan = teachplanRepository.save(teachplan);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName("测试视频");
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
    }
} 