package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, Long> {
    
    List<CourseTeacher> findByOrganizationId(Long organizationId);
    
    @Query("SELECT ct FROM CourseTeacher ct JOIN ct.courses c WHERE c.id = :courseId")
    List<CourseTeacher> findByCourseId(Long courseId);
    
    @Query("SELECT ct FROM CourseTeacher ct JOIN ct.courses c WHERE c.id = :courseId AND ct.organizationId = :organizationId")
    List<CourseTeacher> findByCourseIdAndOrganizationId(Long courseId, Long organizationId);
} 