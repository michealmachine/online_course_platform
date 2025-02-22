package com.double2and9.auth_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * OAuth2 授权服务器配置测试
 */
@SpringBootTest
class AuthorizationServerConfigTest {

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private AuthorizationServerSettings authorizationServerSettings;

    @Test
    void contextLoads() {
        // 验证所有必要的Bean都被正确创建
        assertNotNull(registeredClientRepository, "客户端仓库不能为空");
        assertNotNull(authorizationService, "授权服务不能为空");
        assertNotNull(authorizationConsentService, "授权确认服务不能为空");
        assertNotNull(authorizationServerSettings, "授权服务器设置不能为空");
    }

    @Test
    void authorizationServerSettingsConfigured() {
        // 验证授权服务器设置是否正确
        assertEquals("http://localhost:8084", authorizationServerSettings.getIssuer(), 
            "发行者URL配置不正确");
    }
} 