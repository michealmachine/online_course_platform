package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseTeacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, Long> {

    Page<CourseTeacher> findByOrganizationId(Long organizationId, Pageable pageable);

    @Query("SELECT ct FROM CourseTeacher ct JOIN ct.courses c WHERE c.id = :courseId")
    Page<CourseTeacher> findByCourseId(Long courseId, Pageable pageable);

    @Query("SELECT ct FROM CourseTeacher ct JOIN ct.courses c WHERE c.id = :courseId AND ct.organizationId = :organizationId")
    List<CourseTeacher> findByCourseIdAndOrganizationId(Long courseId, Long organizationId);

    /**
     * 根据机构ID和教师ID查询教师
     */
    Optional<CourseTeacher> findByOrganizationIdAndId(Long organizationId, Long teacherId);
}