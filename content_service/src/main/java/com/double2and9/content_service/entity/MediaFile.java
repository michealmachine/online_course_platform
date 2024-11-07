package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 媒资文件信息
 */
@Data
@Entity
@Table(name = "media_files")
public class MediaFile {
    /**
     * 文件ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件名称
     */
    @Column(nullable = false, length = 255)
    private String fileName;

    /**
     * 文件在文件系统中的路径
     */
    @Column(length = 255)
    private String filePath;

    /**
     * 文件大小，单位字节
     */
    @Column
    private Long fileSize;

    /**
     * 文件类型
     */
    @Column(length = 50)
    private String fileType;

    /**
     * 创建时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    /**
     * 更新时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;

    /**
     * 关联的教学计划媒资信息
     */
    @OneToMany(mappedBy = "mediaFile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeachplanMedia> teachplanMedias;
}