package com.double2and9.media.repository;

import com.double2and9.media.entity.MultipartUploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface MultipartUploadRecordRepository extends JpaRepository<MultipartUploadRecord, String> {
    
    /**
     * 根据上传ID查询记录
     */
    Optional<MultipartUploadRecord> findByUploadId(String uploadId);
    
    /**
     * 根据媒体文件ID查询记录
     */
    Optional<MultipartUploadRecord> findByMediaFileId(String mediaFileId);
    
    /**
     * 根据状态查询记录
     */
    List<MultipartUploadRecord> findByStatus(String status);
    
    /**
     * 查询过期的上传记录
     */
    List<MultipartUploadRecord> findByStatusAndExpirationTimeBefore(String status, Date expirationTime);
    
    /**
     * 更新已上传分片数
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE MultipartUploadRecord m SET m.uploadedChunks = :uploadedChunks, m.updateTime = CURRENT_TIMESTAMP " +
           "WHERE m.uploadId = :uploadId")
    int updateUploadedChunks(String uploadId, Integer uploadedChunks);
    
    /**
     * 更新上传状态
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE MultipartUploadRecord m SET m.status = :status, m.updateTime = CURRENT_TIMESTAMP " +
           "WHERE m.uploadId = :uploadId")
    int updateStatus(String uploadId, String status);
    
    /**
     * 根据机构ID和状态查询记录
     */
    List<MultipartUploadRecord> findByMediaFileIdStartingWithAndStatus(String organizationPrefix, String status);
    
    /**
     * 删除过期的上传记录
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MultipartUploadRecord m WHERE m.status = :status AND m.expirationTime < :expirationTime")
    int deleteExpiredRecords(String status, Date expirationTime);
} 