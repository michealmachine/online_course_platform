package com.double2and9.content_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
     * 审核状态
     * 使用 CourseAuditStatusEnum:
     * 202004: 已提交审核
     * 202005: 审核通过
     * 202006: 审核不通过
     */
    @Column(length = 20)
    private String status;

    /**
     * 审核意见
     */
    @Column(length = 500)
    private String auditMessage;

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
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CourseBase courseBase;
}