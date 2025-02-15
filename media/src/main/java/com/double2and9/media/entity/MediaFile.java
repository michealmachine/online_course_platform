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
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "file_type", discriminatorType = DiscriminatorType.STRING)
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "media_type", length = 12)
    private String mediaType;

    @Column(name = "media_file_id", unique = true, length = 120)
    private String mediaFileId;

    @Column(name = "bucket", length = 255)
    private String bucket;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "url", length = 1024)
    private String url;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(length = 12)
    private String status;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 文件MIME类型
     */
    @Column(length = 128)
    private String mimeType;

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