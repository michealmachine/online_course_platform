package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseAuditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAuditHistoryRepository extends JpaRepository<CourseAuditHistory, Long> {

    /**
     * 查询课程的审核历史
     */
    Page<CourseAuditHistory> findByCourseIdOrderByAuditTimeDesc(Long courseId, Pageable pageable);

    /**
     * 查询审核人的审核历史
     */
    Page<CourseAuditHistory> findByAuditorIdOrderByAuditTimeDesc(Long auditorId, Pageable pageable);
}