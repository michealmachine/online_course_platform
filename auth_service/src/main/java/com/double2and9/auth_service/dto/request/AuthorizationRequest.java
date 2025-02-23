package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    // PKCE 相关字段
    @Pattern(regexp = "^[A-Za-z0-9-._~]{43,128}$", message = "code_challenge 格式不正确")
    private String codeChallenge;
    
    @Pattern(regexp = "^(plain|S256)$", message = "code_challenge_method 必须是 plain 或 S256")
    private String codeChallengeMethod = "S256"; // 默认使用 S256
} 