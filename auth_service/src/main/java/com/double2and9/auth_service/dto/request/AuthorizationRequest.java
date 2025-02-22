package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorizationRequest {
    @NotBlank(message = "响应类型不能为空")
    private String responseType;  // 必须是 "code"

    @NotBlank(message = "客户端ID不能为空")
    private String clientId;

    @NotBlank(message = "重定向URI不能为空")
    private String redirectUri;

    @NotBlank(message = "授权范围不能为空")
    private String scope;

    private String state;  // 可选，用于防止CSRF攻击

    private String codeChallenge;  // PKCE挑战码，可选
    
    private String codeChallengeMethod;  // PKCE挑战方法，可选，支持"S256"和"plain"
} 