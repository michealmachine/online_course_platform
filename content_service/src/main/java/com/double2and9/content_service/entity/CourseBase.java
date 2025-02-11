package com.double2and9.content_service.entity;

import com.double2and9.base.enums.CourseStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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
     * 课程业务状态
     * 使用 CourseStatusEnum:
     * 202001: 草稿
     * 202002: 已发布
     * 202003: 已下线
     */
    @Column(length = 20)
    private String status = CourseStatusEnum.DRAFT.getCode(); // 默认为草稿状态

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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updateTime;

    /**
     * 课程营销信息
     */
    @OneToOne(mappedBy = "courseBase", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CourseMarket courseMarket;

    /**
     * 课程教师列表
     */
    @ManyToMany(mappedBy = "courses", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CourseTeacher> teachers = new HashSet<>();

    /**
     * 课程计划列表
     */
    @OneToMany(mappedBy = "courseBase", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Teachplan> teachplans;

    /**
     * 课程发布信息
     */
    @OneToOne(mappedBy = "courseBase", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CoursePublish coursePublish;

    /**
     * 课程预发布信息
     */
    @OneToOne(mappedBy = "courseBase", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CoursePublishPre coursePublishPre;

    /**
     * 机构ID
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * 审核人ID
     */
    @Column(name = "auditor_id")
    private Long auditorId;
}