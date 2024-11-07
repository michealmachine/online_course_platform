package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.Teachplan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachplanRepository extends JpaRepository<Teachplan, Long> {
    
    // 根据课程ID查询课程计划，按orderBy字段升序排序
    List<Teachplan> findByCourseBaseIdOrderByOrderBy(Long courseId);
    
    // 根据父ID查询子节点
    List<Teachplan> findByParentId(Long parentId);
    
    // 根据父ID查询子节点并排序
    List<Teachplan> findByParentIdOrderByOrderBy(Long parentId);
    
    // 获取课程的第一级章节
    List<Teachplan> findByCourseBaseIdAndLevelOrderByOrderBy(Long courseId, Integer level);
    
    // 统计同一父节点下的子节点数量
    Integer countByParentId(Long parentId);
    
    // 删除某个父节点下的所有子节点
    @Modifying
    void deleteByParentId(Long parentId);
} 