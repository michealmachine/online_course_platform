package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.Teachplan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachplanRepository extends JpaRepository<Teachplan, Long> {
    
    // 根据课程ID查询课程计划，按orderBy字段升序排序
    List<Teachplan> findByCourseBaseIdOrderByOrderBy(Long courseId);
    
    // 查询某个父节点下的所有子节点
    List<Teachplan> findByParentIdOrderByOrderBy(Long parentId);
    
    // 获取课程的第一级章节
    List<Teachplan> findByCourseBaseIdAndLevelOrderByOrderBy(Long courseId, Integer level);
    
    // 自定义查询示例
    @Query("SELECT t FROM Teachplan t WHERE t.courseBase.id = ?1 AND t.parentId = 0 ORDER BY t.orderBy")
    List<Teachplan> findChapters(Long courseId);
} 