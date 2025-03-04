package com.double2and9.auth_service.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenIntrospectionResponse {
    private boolean active;           // 令牌是否有效
    
    @JsonProperty("client_id")
    private String clientId;          // 客户端ID
    
    @JsonProperty("user_id")
    private String userId;            // 用户ID
    
    private String username;          // 用户名
    
    private String scope;             // 权限范围
    private long exp;                 // 过期时间
    private long iat;                 // 签发时间
    
    @JsonProperty("token_type")
    private String tokenType;         // 令牌类型
} 