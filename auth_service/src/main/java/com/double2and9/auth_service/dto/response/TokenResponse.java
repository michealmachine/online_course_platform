package com.double2and9.auth_service.dto.response;

import lombok.Data;

@Data
public class TokenResponse {
    private String accessToken;      // 访问令牌
    private String refreshToken;     // 刷新令牌
    private String tokenType;        // 令牌类型，固定为 "Bearer"
    private int expiresIn;          // 访问令牌过期时间（秒）
    private String scope;            // 授权范围
} 