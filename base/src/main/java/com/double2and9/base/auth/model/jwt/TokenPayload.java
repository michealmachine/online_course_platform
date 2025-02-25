package com.double2and9.base.auth.model.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * JWT令牌载荷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPayload {
    // 标准JWT字段
    private String iss;     // 签发者
    private String sub;     // 主题(通常是用户ID或用户名)
    private String aud;     // 接收方
    private Date exp;       // 过期时间
    private Date nbf;       // 生效时间
    private Date iat;       // 签发时间
    private String jti;     // JWT ID
    
    // 自定义字段
    private String type;    // 令牌类型 (access_token或refresh_token)
    private String userId;  // 用户ID
    private String clientId;// 客户端ID
    private String scope;   // 授权范围
    
    // 其他自定义声明
    private Map<String, Object> additionalClaims;
} 