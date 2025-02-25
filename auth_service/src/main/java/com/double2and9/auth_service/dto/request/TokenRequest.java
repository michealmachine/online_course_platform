package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRequest {
    @NotBlank(message = "授权类型不能为空")
    private String grantType;  // 授权类型，支持 authorization_code 和 refresh_token

    // authorization_code 模式需要的字段
    private String code;  // 授权码
    private String redirectUri;  // 重定向URI
    
    // refresh_token 模式需要的字段
    private String refreshToken;  // 刷新令牌
    
    // PKCE 扩展需要的字段
    private String codeVerifier;  // PKCE验证码
} 