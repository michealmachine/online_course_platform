package com.double2and9.media.repository;

import com.double2and9.media.entity.MultipartUploadRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class MultipartUploadRecordRepositoryTest {

    @Autowired
    private MultipartUploadRecordRepository repository;

    /**
     * 创建测试用的上传记录
     */
    private MultipartUploadRecord createTestRecord() {
        MultipartUploadRecord record = new MultipartUploadRecord();
        record.setUploadId(UUID.randomUUID().toString());
        record.setMediaFileId("test-media-" + UUID.randomUUID().toString());
        record.setFileName("test-video.mp4");
        record.setFileSize(1024L * 1024L); // 1MB
        record.setBucket("test-bucket");
        record.setFilePath("/test/path/video.mp4");
        record.setTotalChunks(10);
        record.setUploadedChunks(0);
        record.setChunkSize(1024 * 1024); // 1MB
        record.setStatus("UPLOADING");
        record.setExpirationTime(new Date(System.currentTimeMillis() + 3600000)); // 1小时后过期
        record.setInitiateTime(new Date());
        
        return repository.save(record);
    }

    @Test
    public void testSaveAndFind() {
        // 创建测试记录
        MultipartUploadRecord saved = createTestRecord();
        
        // 测试findByUploadId
        Optional<MultipartUploadRecord> found = repository.findByUploadId(saved.getUploadId());
        assertThat(found).isPresent();
        assertThat(found.get().getFileName()).isEqualTo(saved.getFileName());
        
        // 测试findByMediaFileId
        Optional<MultipartUploadRecord> foundByMediaId = repository.findByMediaFileId(saved.getMediaFileId());
        assertThat(foundByMediaId).isPresent();
        assertThat(foundByMediaId.get().getUploadId()).isEqualTo(saved.getUploadId());
    }

    @Test
    public void testUpdateUploadedChunks() {
        // 创建测试记录
        MultipartUploadRecord saved = createTestRecord();
        
        // 更新已上传分片数
        int updated = repository.updateUploadedChunks(saved.getUploadId(), 5);
        assertThat(updated).isEqualTo(1);
        
        // 验证更新结果
        Optional<MultipartUploadRecord> found = repository.findByUploadId(saved.getUploadId());
        assertThat(found).isPresent();
        assertThat(found.get().getUploadedChunks()).isEqualTo(5);
    }

    @Test
    public void testUpdateStatus() {
        // 创建测试记录
        MultipartUploadRecord saved = createTestRecord();
        
        // 更新状态
        int updated = repository.updateStatus(saved.getUploadId(), "COMPLETED");
        assertThat(updated).isEqualTo(1);
        
        // 验证更新结果
        Optional<MultipartUploadRecord> found = repository.findByUploadId(saved.getUploadId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    public void testFindExpiredRecords() {
        // 创建一个已过期的记录
        MultipartUploadRecord record = createTestRecord();
        record.setExpirationTime(new Date(System.currentTimeMillis() - 3600000)); // 1小时前过期
        repository.save(record);
        
        // 查询过期记录
        List<MultipartUploadRecord> expired = repository.findByStatusAndExpirationTimeBefore(
            "UPLOADING", new Date());
        
        assertThat(expired).isNotEmpty();
        assertThat(expired.get(0).getUploadId()).isEqualTo(record.getUploadId());
    }

    @Test
    public void testDeleteExpiredRecords() {
        // 创建一个已过期的记录
        MultipartUploadRecord record = createTestRecord();
        record.setExpirationTime(new Date(System.currentTimeMillis() - 3600000)); // 1小时前过期
        repository.save(record);
        
        // 删除过期记录
        int deleted = repository.deleteExpiredRecords("UPLOADING", new Date());
        assertThat(deleted).isGreaterThan(0);
        
        // 验证记录已被删除
        Optional<MultipartUploadRecord> found = repository.findByUploadId(record.getUploadId());
        assertThat(found).isEmpty();
    }

    @Test
    public void testFindByOrganizationAndStatus() {
        // 创建测试记录，mediaFileId包含机构ID前缀
        MultipartUploadRecord record = createTestRecord();
        record.setMediaFileId("org_123_" + UUID.randomUUID().toString());
        repository.save(record);
        
        // 查询指定机构的记录
        List<MultipartUploadRecord> records = repository.findByMediaFileIdStartingWithAndStatus(
            "org_123_", "UPLOADING");
        
        assertThat(records).isNotEmpty();
        assertThat(records.get(0).getMediaFileId()).startsWith("org_123_");
    }
} 