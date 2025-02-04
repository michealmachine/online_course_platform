package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.MediaFile;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.entity.TeachplanMedia;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Test
    void testSaveTeachplanMedia() {
        // 准备测试数据
        Teachplan teachplan = new Teachplan();
        teachplan.setName("测试章节");
        teachplan = teachplanRepository.save(teachplan);

        // 创建MediaFile
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaFileId("test123"); // 设置mediaFileId作为主键
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
        mediaFile.setMediaFileId("test456"); // 设置不同的mediaFileId
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

    @Test
    void testFindPageByTeachplanId() {
        // 创建课程和课程计划（只创建一次，复用同一个课程计划）
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase = courseBaseRepository.save(courseBase);

        Teachplan teachplan = new Teachplan();
        teachplan.setName("测试章节");
        teachplan.setCourseBase(courseBase);
        teachplan.setLevel(1);
        teachplan.setOrderBy(1);
        teachplan.setCreateTime(new Date());
        teachplan.setUpdateTime(new Date());
        teachplan = teachplanRepository.save(teachplan);

        // 创建两个媒资文件并关联到同一个课程计划
        MediaFile mediaFile1 = createMediaFile("视频1");
        TeachplanMedia media1 = createTeachplanMedia(teachplan, mediaFile1);

        MediaFile mediaFile2 = createMediaFile("视频2");
        TeachplanMedia media2 = createTeachplanMedia(teachplan, mediaFile2);

        // 测试分页查询
        Page<TeachplanMedia> result = teachplanMediaRepository.findPageByTeachplanId(
                teachplan.getId(),
                PageRequest.of(0, 10));

        // 验证结果
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .map(m -> m.getMediaFile().getFileName())
                .anyMatch(name -> name.equals("视频1")));
    }

    /**
     * 创建测试用的 MediaFile
     */
    private MediaFile createMediaFile(String fileName) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaFileId("test-" + fileName);
        mediaFile.setOrganizationId(TEST_ORG_ID);
        mediaFile.setFileName(fileName);
        mediaFile.setMediaType("VIDEO");
        mediaFile.setCreateTime(new Date());
        mediaFile.setUpdateTime(new Date());
        return mediaFileRepository.save(mediaFile);
    }

    /**
     * 创建测试用的 TeachplanMedia
     */
    private TeachplanMedia createTeachplanMedia(Teachplan teachplan, MediaFile mediaFile) {
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setTeachplan(teachplan);
        teachplanMedia.setMediaFile(mediaFile);
        teachplanMedia.setCreateTime(new Date());
        teachplanMedia.setUpdateTime(new Date());
        return teachplanMediaRepository.save(teachplanMedia);
    }
}