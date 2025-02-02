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
     * 发布状态
     * 例如：未发布、已发布、下线等
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