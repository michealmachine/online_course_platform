package com.double2and9.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "oauth2_authorization_code")
public class AuthorizationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;  // 授权码

    @Column(nullable = false)
    private String clientId;  // 客户端ID

    @Column(nullable = false)
    private String userId;  // 用户ID

    @Column(nullable = false)
    private String redirectUri;  // 重定向URI

    @Column(nullable = false)
    private String scope;  // 授权范围

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // 过期时间

    @Column
    private String codeChallenge;  // PKCE挑战码

    @Column
    private String codeChallengeMethod;  // PKCE挑战方法

    @Column(nullable = false)
    private boolean used;  // 是否已使用
} 