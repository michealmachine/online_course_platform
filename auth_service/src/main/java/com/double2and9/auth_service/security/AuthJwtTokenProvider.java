package com.double2and9.auth_service.security;

import com.double2and9.base.config.JwtProperties;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthJwtTokenProvider extends com.double2and9.base.security.JwtTokenProvider {

    public AuthJwtTokenProvider(JwtProperties jwtProperties) {
        super(jwtProperties);  // 调用父类构造函数
    }

    /**
     * 生成用户认证令牌
     */
    @Override
    public String generateToken(Authentication authentication) {
        SecurityUser userPrincipal = (SecurityUser) authentication.getPrincipal();
        return generateToken(Map.of("sub", userPrincipal.getUsername()), getJwtProperties().getExpiration() / 1000L);
    }
} 