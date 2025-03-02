package com.double2and9.auth_service.repository;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Primary
@Repository
public class CustomJdbcRegisteredClientRepository extends JdbcRegisteredClientRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RegisteredClient> registeredClientRowMapper;

    public CustomJdbcRegisteredClientRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
        this.registeredClientRowMapper = new JdbcRegisteredClientRepository.RegisteredClientRowMapper();
    }

    /**
     * 保存客户端，支持内部客户端标识和自动授权标识
     */
    @Transactional
    public void save(RegisteredClient registeredClient, Boolean isInternal, Boolean autoApprove) {
        // 先调用父类方法保存基本信息
        super.save(registeredClient);
        
        // 更新内部客户端标识和自动授权标识
        if (isInternal != null || autoApprove != null) {
            String sql = "UPDATE oauth2_registered_client SET";
            boolean hasIsInternal = isInternal != null;
            boolean hasAutoApprove = autoApprove != null;
            
            if (hasIsInternal) {
                sql += " is_internal = ?";
            }
            
            if (hasIsInternal && hasAutoApprove) {
                sql += ",";
            }
            
            if (hasAutoApprove) {
                sql += " auto_approve = ?";
            }
            
            sql += " WHERE id = ?";
            
            if (hasIsInternal && hasAutoApprove) {
                jdbcTemplate.update(sql, isInternal, autoApprove, registeredClient.getId());
            } else if (hasIsInternal) {
                jdbcTemplate.update(sql, isInternal, registeredClient.getId());
            } else if (hasAutoApprove) {
                jdbcTemplate.update(sql, autoApprove, registeredClient.getId());
            }
        }
    }

    /**
     * 检查客户端是否为内部客户端
     */
    public boolean isInternalClient(String clientId) {
        String sql = "SELECT is_internal FROM oauth2_registered_client WHERE client_id = ?";
        List<Boolean> results = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getBoolean("is_internal"), clientId);
        return !results.isEmpty() && results.get(0);
    }

    /**
     * 检查客户端是否自动授权
     */
    public boolean isAutoApproveClient(String clientId) {
        String sql = "SELECT auto_approve FROM oauth2_registered_client WHERE client_id = ?";
        List<Boolean> results = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getBoolean("auto_approve"), clientId);
        return !results.isEmpty() && results.get(0);
    }

    /**
     * 查询所有客户端
     */
    public List<RegisteredClient> findAll() {
        String sql = "SELECT * FROM oauth2_registered_client";
        return jdbcTemplate.query(sql, registeredClientRowMapper);
    }

    /**
     * 根据ID删除客户端
     */
    @Transactional
    public void deleteById(String id) {
        String sql = "DELETE FROM oauth2_registered_client WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
} 