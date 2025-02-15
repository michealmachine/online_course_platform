package com.double2and9.media.repository;

import com.double2and9.media.entity.VideoFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class VideoFileRepositoryTest {

    @Autowired
    private VideoFileRepository videoFileRepository;

    @Test
    public void testSaveAndFindVideoFile() {
        // 准备测试数据
        VideoFile videoFile = new VideoFile();
        videoFile.setOrganizationId(1L);
        videoFile.setFileName("test-video.mp4");
        videoFile.setMediaType("video");
        videoFile.setMediaFileId("test-media-id");
        videoFile.setBucket("test-bucket");
        videoFile.setFilePath("/test/path/video.mp4");
        videoFile.setFileSize(1024L);
        videoFile.setStatus("UPLOADED");
        videoFile.setMimeType("video/mp4");
        videoFile.setPurpose("TEST");
        
        // 设置视频特有属性
        videoFile.setDuration(120L); // 2分钟
        videoFile.setWidth(1920);
        videoFile.setHeight(1080);
        videoFile.setCodec("H.264");
        videoFile.setBitrate(2048L);

        // 保存视频文件
        VideoFile savedFile = videoFileRepository.save(videoFile);
        assertThat(savedFile.getId()).isNotNull();

        // 测试根据mediaFileId查询
        VideoFile foundByMediaFileId = videoFileRepository.findByMediaFileId("test-media-id");
        assertThat(foundByMediaFileId).isNotNull();
        assertThat(foundByMediaFileId.getFileName()).isEqualTo("test-video.mp4");

        // 测试根据组织ID和文件名查询
        VideoFile foundByOrgAndFileName = videoFileRepository.findByOrganizationIdAndFileName(1L, "test-video.mp4");
        assertThat(foundByOrgAndFileName).isNotNull();
        assertThat(foundByOrgAndFileName.getMediaFileId()).isEqualTo("test-media-id");
        
        // 验证视频特有属性
        assertThat(foundByOrgAndFileName.getDuration()).isEqualTo(120L);
        assertThat(foundByOrgAndFileName.getWidth()).isEqualTo(1920);
        assertThat(foundByOrgAndFileName.getHeight()).isEqualTo(1080);
        assertThat(foundByOrgAndFileName.getCodec()).isEqualTo("H.264");
        assertThat(foundByOrgAndFileName.getBitrate()).isEqualTo(2048L);
    }
} 