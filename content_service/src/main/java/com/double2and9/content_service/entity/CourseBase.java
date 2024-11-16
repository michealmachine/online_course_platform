package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 课程基本信息
 */
@Data
@Entity
@Table(name = "course_base")
public class CourseBase {
    /**
     * 课程ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 课程名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 课程简介
     */
    @Column(columnDefinition = "TEXT")
    private String brief;

    /**
     * 课程封面图片URL，来自文件系统的访问地址
     */
    @Column(length = 1024)
    private String logo;

    /**
     * 课程大分类
     */
    @Column
    private Long mt;

    /**
     * 课程小分类
     */
    @Column
    private Long st;

    /**
     * 原价
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal priceOld;

    /**
     * 现价
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 收费规则，对应数据字典
     */
    @Column(length = 20)
    private String charge;

    /**
     * 课程状态
     * 对应数据字典: course_status
     * 例如：未发布、已发布、已下线等
     */
    @Column(length = 20)
    private String status;

    /**
     * 课程有效性，true有效，false无效
     */
    @Column
    private Boolean valid;

    /**
     * 咨询QQ
     */
    @Column(length = 20)
    private String qq;

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
     * 课程营销信息
     */
    @OneToOne(mappedBy = "courseBase", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private CourseMarket courseMarket;

    /**
     * 课程教师列表
     */
    @OneToMany(mappedBy = "courseBase", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseTeacher> teachers;

    /**
     * 课程计划列表
     */
    @OneToMany(mappedBy = "courseBase", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Teachplan> teachplans;

    /**
     * 课程发布信息
     */
    @OneToOne(mappedBy = "courseBase", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private CoursePublish coursePublish;

    /**
     * 课程预发布信息
     */
    @OneToOne(mappedBy = "courseBase", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private CoursePublishPre coursePublishPre;

    /**
     * 机构ID
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
}