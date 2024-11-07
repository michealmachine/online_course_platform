package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, Long> {
    
    // 根据课程ID查询教师列表
    List<CourseTeacher> findByCourseBaseId(Long courseId);
    
    // 根据教师名称模糊查询
    List<CourseTeacher> findByNameContaining(String name);
    
    // 根据职位查询
    List<CourseTeacher> findByPosition(String position);
    
    // 根据课程ID和教师名称查询
    List<CourseTeacher> findByCourseBaseIdAndName(Long courseId, String name);
} 