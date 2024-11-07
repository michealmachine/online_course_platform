package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

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
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 教师职位
     */
    @Column(length = 255)
    private String position;

    /**
     * 教师简介
     */
    @Column(columnDefinition = "TEXT")
    private String description;

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
     * 所属课程
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    private CourseBase courseBase;
}