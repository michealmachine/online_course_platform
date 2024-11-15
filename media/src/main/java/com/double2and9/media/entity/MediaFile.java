package com.double2and9.media.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 媒资文件实体
 */
@Data
@ToString
@Entity
@Table(name = "media_file")
public class MediaFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "company_id")
    private Long companyId;              // 机构ID，为后续多租户做准备
    
    @Column(name = "company_name", length = 255)
    private String companyName;          // 机构名称
    
    @Column(name = "file_name", length = 255)
    private String fileName;             // 文件名称
    
    @Column(name = "file_type", length = 12)
    private String fileType;             // 文件类型
    
    @Column(name = "file_id", unique = true, length = 120)
    private String fileId;               // 文件唯一标识
    
    @Column(name = "bucket", length = 255)
    private String bucket;               // MinIO存储桶
    
    @Column(name = "file_path", length = 512)
    private String filePath;             // 文件存储路径
    
    @Column(name = "url", length = 1024)
    private String url;                  // 文件访问地址
    
    @Column(name = "file_size")
    private Long fileSize;               // 文件大小（字节）
    
    @Column(length = 12)
    private String status;               // 状态（1：正常，0：不显示）
    
    @Column(name = "audit_status", length = 12)
    private String auditStatus;          // 审核状态
    
    @Column(name = "audit_mind", length = 255)
    private String auditMind;            // 审核意见
    
    @Column(name = "create_time")
    private Date createTime;             // 创建时间
    
    @Column(name = "update_time")
    private Date updateTime;             // 更新时间
    
    /**
     * 文件MD5值，用于文件去重和完整性校验
     */
    @Column(length = 32)
    private String fileMd5;
    
    /**
     * 文件MIME类型
     */
    @Column(length = 128)
    private String mimeType;
    
    /**
     * 上传人
     */
    @Column(length = 64)
    private String uploadUser;
    
    /**
     * 标签，用于分类和检索，多个标签用逗号分隔
     */
    @Column(length = 512)
    private String tags;
    
    /**
     * 文件用途，例如:课程封面、课程视频等
     */
    @Column(length = 32)
    private String purpose;

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