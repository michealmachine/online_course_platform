package com.double2and9.auth_service.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenIntrospectionResponse {
    private boolean active;           // 令牌是否有效
    private String clientId;          // 客户端ID
    private String userId;            // 用户ID
    private String scope;             // 权限范围
    private long exp;                 // 过期时间
    private long iat;                 // 签发时间
    private String tokenType;         // 令牌类型
} 