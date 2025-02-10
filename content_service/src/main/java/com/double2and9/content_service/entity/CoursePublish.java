package com.double2and9.content_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

/**
 * 课程发布信息
 */
@Data
@Entity
@Table(name = "course_publish")
public class CoursePublish {
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
     * 课程业务状态
     * 使用 CourseStatusEnum:
     * 202002: 已发布
     * 202003: 已下线
     * 注：发布记录只能是已发布或已下线状态
     */
    @Column(length = 20)
    private String status;

    /**
     * 课程发布时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date publishTime;

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
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CourseBase courseBase;
}