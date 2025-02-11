package com.double2and9.content_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 媒资文件信息
 */
@Data
@Entity
@Table(name = "media_files")
public class MediaFile {
    /**
     * 主键，使用media服务的fileId
     */
    @Id
    @Column(length = 32)
    private String mediaFileId;

    /**
     * 机构ID
     */
    @Column(nullable = false)
    private Long organizationId;

    /**
     * 文件名称
     */
    @Column(length = 255)
    private String fileName;

    /**
     * 本地审核状态
     */
    @Column(length = 20)
    private String auditStatus;

    /**
     * 审核信息
     */
    @Column(length = 255)
    private String auditMessage;

    /**
     * 媒体类型：IMAGE/VIDEO
     */
    @Column(length = 20)
    private String mediaType;

    /**
     * 访问地址
     */
    @Column(length = 512)
    private String url;

    /**
     * 文件大小
     */
    @Column
    private Long fileSize;

    /**
     * 文件类型
     */
    @Column(length = 128)
    private String mimeType;

    /**
     * 文件用途：COVER(封面),VIDEO(视频)等
     */
    @Column(length = 32)
    private String purpose;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (updateTime == null) {
            updateTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }

    /**
     * 关联的教学计划媒资信息
     */
    @OneToMany(mappedBy = "mediaFile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeachplanMedia> teachplanMedias;
}