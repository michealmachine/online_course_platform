package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.Teachplan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * 根据课程ID和父节点ID查询课程计划列表，按orderBy排序
     */
    List<Teachplan> findByCourseBaseIdAndParentIdOrderByOrderBy(Long courseId, Long parentId);

    /**
     * 查找同一父节点下的最大排序号
     */
    @Query("SELECT MAX(t.orderBy) FROM Teachplan t WHERE t.parentId = :parentId")
    Integer findMaxOrderByUnderParent(@Param("parentId") Long parentId);

    /**
     * 查找同级的上一个节点
     */
    @Query("SELECT t FROM Teachplan t WHERE t.parentId = :parentId AND t.orderBy < :orderBy ORDER BY t.orderBy DESC")
    Optional<Teachplan> findPreviousNode(@Param("parentId") Long parentId, @Param("orderBy") Integer orderBy);

    /**
     * 查找同级的下一个节点
     */
    @Query("SELECT t FROM Teachplan t WHERE t.parentId = :parentId AND t.orderBy > :orderBy ORDER BY t.orderBy ASC")
    Optional<Teachplan> findNextNode(@Param("parentId") Long parentId, @Param("orderBy") Integer orderBy);

    /**
     * 分页查询课程计划
     */
    @Query("SELECT t FROM Teachplan t WHERE t.courseBase.id = :courseId")
    Page<Teachplan> findPageByCourseId(@Param("courseId") Long courseId, Pageable pageable);

    /**
     * 分页查询某一级别的课程计划
     */
    Page<Teachplan> findByCourseBaseIdAndLevel(Long courseId, Integer level, Pageable pageable);
}