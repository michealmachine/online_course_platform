package com.double2and9.media.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 分片上传记录实体
 */
@Data
@ToString
@Entity
@Table(name = "multipart_upload_record")
public class MultipartUploadRecord {
    
    @Id
    @Column(name = "upload_id", length = 255)
    private String uploadId;              // 分片上传ID，使用S3返回的uploadId
    
    @Column(name = "media_file_id", length = 120)
    private String mediaFileId;           // 关联的媒体文件ID
    
    @Column(name = "organization_id")
    private Long organizationId;          // 机构ID
    
    @Column(name = "file_name", length = 255)
    private String fileName;              // 文件名
    
    @Column(name = "file_size")
    private Long fileSize;                // 文件总大小
    
    @Column(name = "bucket", length = 255)
    private String bucket;                // S3存储桶
    
    @Column(name = "file_path", length = 512)
    private String filePath;              // S3文件路径
    
    @Column(name = "media_type", length = 32)
    private String mediaType;             // 媒体类型（VIDEO/IMAGE等）
    
    @Column(name = "mime_type", length = 128)
    private String mimeType;              // MIME类型
    
    @Column(name = "purpose", length = 32)
    private String purpose;               // 用途
    
    @Column(name = "total_chunks")
    private Integer totalChunks;          // 总分片数
    
    @Column(name = "chunk_size")
    private Integer chunkSize;            // 分片大小
    
    @Column(name = "status", length = 32)
    private String status;                // 上传状态
    
    @Column(name = "create_time")
    private Date createTime;              // 创建时间
    
    @Column(name = "update_time")
    private Date updateTime;              // 更新时间
    
    @Column(name = "expiration_time")
    private Date expirationTime;          // 过期时间
    
    @Column(name = "initiate_time")
    private Date initiateTime;            // 初始化上传时间
    
    @Column(name = "complete_time")
    private Date completeTime;            // 完成上传时间
    
    @Column(name = "abort_time")
    private Date abortTime;               // 中止上传时间

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = new Date();
        }
        if (updateTime == null) {
            updateTime = new Date();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Date();
    }
} 