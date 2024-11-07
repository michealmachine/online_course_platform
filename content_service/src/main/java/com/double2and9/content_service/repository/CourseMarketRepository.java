package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseMarket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CourseMarketRepository extends JpaRepository<CourseMarket, Long> {
    
    // 查询特定价格区间的课程
    List<CourseMarket> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    // 查询免费课程
    List<CourseMarket> findByPrice(BigDecimal price);
    
    // 查询有效的课程营销信息
    List<CourseMarket> findByValid(Boolean valid);
} 