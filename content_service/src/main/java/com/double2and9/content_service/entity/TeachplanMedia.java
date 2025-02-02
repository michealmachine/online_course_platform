package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

/**
 * 课程计划与媒资关联信息
 */
@Data
@Entity
@Table(name = "teachplan_media")
public class TeachplanMedia {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 课程计划
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teachplan_id", referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Teachplan teachplan;

    /**
     * 媒资文件
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", referencedColumnName = "mediaFileId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MediaFile mediaFile;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private Date createTime;

    /**
     * 更新时间
     */
    @Column(nullable = false)
    private Date updateTime;

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