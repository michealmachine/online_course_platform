package com.double2and9.auth_service.repository;

import com.double2and9.auth_service.entity.AuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthorizationCodeRepository extends JpaRepository<AuthorizationCode, Long> {
    Optional<AuthorizationCode> findByCode(String code);
    
    /**
     * 查询指定用户和客户端的最新未使用且未过期的授权码
     *
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param currentTime 当前时间
     * @return 授权码对象
     */
    Optional<AuthorizationCode> findFirstByUserIdAndClientIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String userId, String clientId, LocalDateTime currentTime);
} 