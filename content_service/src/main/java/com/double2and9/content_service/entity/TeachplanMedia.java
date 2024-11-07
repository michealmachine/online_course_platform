package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
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
     * 课程计划
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teachplan_id", referencedColumnName = "id")
    private Teachplan teachplan;

    /**
     * 媒资文件
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", referencedColumnName = "id")
    private MediaFile mediaFile;
}