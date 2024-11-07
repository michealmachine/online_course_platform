package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseBaseRepository extends JpaRepository<CourseBase, Long>, JpaSpecificationExecutor<CourseBase> {
    
    // 根据课程名称模糊查询
    List<CourseBase> findByNameContaining(String name);
    
    // 根据课程状态查询
    List<CourseBase> findByValid(Boolean valid);
    
    // 根据课程分类查询
    List<CourseBase> findByMtAndSt(Long mt, Long st);
    
    // 自定义查询示例
    @Query("SELECT c FROM CourseBase c WHERE c.mt = ?1 AND c.valid = true ORDER BY c.createTime DESC")
    List<CourseBase> findLatestCoursesByMt(Long mt);
} 