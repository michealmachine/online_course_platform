package com.double2and9.content_service.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程营销信息
 */
@Data
@Entity
@Table(name = "course_market")
public class CourseMarket {
    /**
     * 主键，课程ID
     */
    @Id
    private Long id;

    /**
     * 收费规则，对应数据字典
     */
    @Column(length = 20)
    private String charge;

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
     * 优惠描述
     */
    @Column(length = 255)
    private String discounts;

    /**
     * 营销信息有效性，true有效，false无效
     */
    @Column
    private Boolean valid;

    /**
     * 创建时间
     */
    @Column
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column
    private LocalDateTime updateTime;

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