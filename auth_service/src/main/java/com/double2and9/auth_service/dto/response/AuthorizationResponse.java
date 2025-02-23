package com.double2and9.auth_service.dto.response;

import lombok.Data;

import java.util.Set;

@Data
public class AuthorizationResponse {
    private String clientId;
    private String clientName;
    private Set<String> requestedScopes;  // 客户端请求的权限范围
    private String state;  // 原样返回state
    private String authorizationId;  // 用于标识本次授权请求
    private String redirectUri;  // 重定向URI
    
    // 添加PKCE相关字段
    private String codeChallenge;  // PKCE挑战码
    private String codeChallengeMethod;  // PKCE挑战方法
} 