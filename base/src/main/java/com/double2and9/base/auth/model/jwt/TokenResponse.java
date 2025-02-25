package com.double2and9.base.auth.model.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 令牌响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;     // 访问令牌
    private String refreshToken;    // 刷新令牌
    private String tokenType;       // 令牌类型，通常是Bearer
    private int expiresIn;          // 过期时间(秒)
    private String scope;           // 授权范围
} 