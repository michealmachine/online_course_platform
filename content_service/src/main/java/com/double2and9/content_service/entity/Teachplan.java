package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.Date;

/**
 * 课程计划（大纲）
 */
@Data
@Entity
@Table(name = "teachplan")
public class Teachplan {
    /**
     * 计划ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 课程计划名称
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 父级ID，如果是章节则为0
     */
    @Column
    private Long parentId;

    /**
     * 层级，1：章节，2：小节
     */
    @Column
    private Integer level;

    /**
     * 排序字段
     */
    @Column
    private Integer orderBy;

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

    /**
     * 关联的媒资信息
     */
    @OneToMany(mappedBy = "teachplan", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeachplanMedia> teachplanMedias;
}