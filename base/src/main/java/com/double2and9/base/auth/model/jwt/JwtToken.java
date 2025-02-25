package com.double2and9.base.auth.model.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT令牌
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtToken {
    private String token;           // JWT令牌字符串
    private TokenPayload payload;   // 解析后的载荷
    private long expiresIn;         // 过期时间(秒)
    private String tokenType;       // 令牌类型(通常是Bearer)
} 