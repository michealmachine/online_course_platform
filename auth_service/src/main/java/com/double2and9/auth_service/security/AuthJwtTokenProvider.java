package com.double2and9.auth_service.security;

import com.double2and9.base.config.JwtProperties;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthJwtTokenProvider extends com.double2and9.base.security.JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public AuthJwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成用户认证令牌
     */
    @Override
    public String generateToken(Authentication authentication) {
        SecurityUser userPrincipal = (SecurityUser) authentication.getPrincipal();
        return generateToken(Map.of("sub", userPrincipal.getUsername()), jwtProperties.getExpiration() / 1000L);
    }
} 