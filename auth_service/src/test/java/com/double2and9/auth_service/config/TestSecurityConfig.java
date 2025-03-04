package com.double2and9.auth_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

/**
 * 测试环境专用的安全配置
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public AuthorizationServerSettings testAuthorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://localhost:8084")
            .build();
    }
} 