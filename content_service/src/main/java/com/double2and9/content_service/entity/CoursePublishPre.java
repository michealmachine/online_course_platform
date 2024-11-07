package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

/**
 * 课程预发布信息
 */
@Data
@Entity
@Table(name = "course_publish_pre")
public class CoursePublishPre {
    /**
     * 主键，课程ID
     */
    @Id
    private Long id;

    /**
     * 课程名称
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 预发布状态
     * 例如：已提交、审核中、审核通过、审核未通过等
     */
    @Column(length = 20)
    private String status;

    /**
     * 预览时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date previewTime;

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
     * 对应的课程基本信息
     */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private CourseBase courseBase;
}