package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    
    // 查找顶级分类
    List<CourseCategory> findByParentId(Long parentId);
    
    // 根据层级查询分类
    List<CourseCategory> findByLevel(Integer level);
    
    // 根据父ID和层级查询
    List<CourseCategory> findByParentIdAndLevel(Long parentId, Integer level);
} 