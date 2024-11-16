package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 课程教师信息
 */
@Data
@Entity
@Table(name = "course_teacher")
public class CourseTeacher {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 教师名称
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * 教师名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 教师职位
     */
    private String position;

    /**
     * 教师简介
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 所属课程
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_teacher_relation",
        joinColumns = @JoinColumn(name = "teacher_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CourseBase> courses = new HashSet<>();

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
}