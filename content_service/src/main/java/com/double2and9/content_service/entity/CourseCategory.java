package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

/**
 * 课程分类
 */
@Data
@Entity
@Table(name = "course_category")
public class CourseCategory {
    /**
     * 分类ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分类名称
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 父级分类ID
     */
    @Column
    private Long parentId;

    /**
     * 分类层级
     */
    @Column
    private Integer level;

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
}