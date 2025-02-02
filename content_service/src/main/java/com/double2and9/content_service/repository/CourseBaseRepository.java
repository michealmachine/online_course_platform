package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // 添加按机构ID查询的方法
    Page<CourseBase> findByOrganizationId(Long organizationId, Pageable pageable);

    // 按机构ID和其他条件查询
    @Query(value = "SELECT c FROM CourseBase c WHERE " +
            "(:organizationId IS NULL OR c.organizationId = :organizationId) AND " +
            "(:courseName IS NULL OR c.name LIKE %:courseName%) AND " +
            "(:status IS NULL OR c.status = :status)", countQuery = "SELECT COUNT(c) FROM CourseBase c WHERE " +
                    "(:organizationId IS NULL OR c.organizationId = :organizationId) AND " +
                    "(:courseName IS NULL OR c.name LIKE %:courseName%) AND " +
                    "(:status IS NULL OR c.status = :status)")
    Page<CourseBase> findByConditions(
            @Param("organizationId") Long organizationId,
            @Param("courseName") String courseName,
            @Param("status") String status,
            Pageable pageable);
}