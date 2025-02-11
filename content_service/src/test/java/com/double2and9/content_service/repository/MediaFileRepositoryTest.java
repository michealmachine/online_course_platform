package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.MediaFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class MediaFileRepositoryTest {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Test
    void testFindByOrganizationIdAndMediaTypeAndPurpose() {
        // 准备测试数据
        MediaFile file1 = new MediaFile();
        file1.setMediaFileId("test1");
        file1.setOrganizationId(TEST_ORG_ID);
        file1.setMediaType("VIDEO");
        file1.setPurpose("COURSE");
        file1.setCreateTime(LocalDateTime.now());
        file1.setUpdateTime(LocalDateTime.now());
        mediaFileRepository.save(file1);

        MediaFile file2 = new MediaFile();
        file2.setMediaFileId("test2");
        file2.setOrganizationId(TEST_ORG_ID);
        file2.setMediaType("IMAGE");
        file2.setPurpose("COVER");
        file2.setCreateTime(LocalDateTime.now());
        file2.setUpdateTime(LocalDateTime.now());
        mediaFileRepository.save(file2);

        // 测试分页查询
        Page<MediaFile> result = mediaFileRepository.findByOrganizationIdAndMediaTypeAndPurpose(
                TEST_ORG_ID,
                "VIDEO",
                "COURSE",
                PageRequest.of(0, 10));

        // 验证结果
        assertEquals(1, result.getTotalElements());
        assertEquals("VIDEO", result.getContent().get(0).getMediaType());
    }
}