package com.double2and9.auth_service.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

/**
 * OIDC用户信息认证上下文
 * 用于在生成用户信息端点响应时提供必要的上下文信息
 */
public class OidcUserInfoAuthenticationContext {
    private final Authentication authentication;
    private final OAuth2Authorization authorization;

    /**
     * 创建一个OIDC用户信息认证上下文
     *
     * @param authentication 用户认证信息
     * @param authorization OAuth2授权信息
     */
    public OidcUserInfoAuthenticationContext(Authentication authentication, OAuth2Authorization authorization) {
        this.authentication = authentication;
        this.authorization = authorization;
    }

    /**
     * 获取用户认证信息
     *
     * @return 用户认证信息
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * 获取OAuth2授权信息
     *
     * @return OAuth2授权信息
     */
    public OAuth2Authorization getAuthorization() {
        return authorization;
    }
} 