package com.double2and9.auth_service.repository;

import com.double2and9.auth_service.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 权限数据访问层
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * 检查权限名是否已存在
     *
     * @param name 权限名
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据资源类型和操作类型模糊查询权限
     *
     * @param resource 资源类型
     * @param action 操作类型
     * @param pageable 分页参数
     * @return 分页的权限列表
     */
    Page<Permission> findByResourceContainingAndActionContaining(
            String resource, String action, Pageable pageable);

    /**
     * 根据权限名查找权限
     *
     * @param name 权限名
     * @return 权限对象
     */
    Optional<Permission> findByName(String name);
} 