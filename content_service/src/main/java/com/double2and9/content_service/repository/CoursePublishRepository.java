package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CoursePublish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursePublishRepository extends JpaRepository<CoursePublish, Long> {
    
    // 根据状态查询
    List<CoursePublish> findByStatus(String status);
    
    // 根据课程名称模糊查询
    List<CoursePublish> findByNameContaining(String name);
} 