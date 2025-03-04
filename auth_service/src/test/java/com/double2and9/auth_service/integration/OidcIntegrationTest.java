package com.double2and9.auth_service.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OIDC 相关端点的集成测试
 * 继承自 BaseOAuth2IntegrationTest 获取 OAuth2 授权
 */
public class OidcIntegrationTest extends BaseOAuth2IntegrationTest {

    @Test
    @DisplayName("测试获取用户信息端点")
    @Transactional
    public void testGetUserInfo() throws Exception {
        mockMvc.perform(get("/api/oauth2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").exists())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.preferred_username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email_verified").exists())
                .andExpect(jsonPath("$.given_name").value(testUser.getGivenName()))
                .andExpect(jsonPath("$.family_name").value(testUser.getFamilyName()));
    }

    @Test
    @DisplayName("测试获取 OIDC 配置")
    @Transactional
    public void testGetOpenIDConfiguration() throws Exception {
        mockMvc.perform(get("/api/oauth2/.well-known/openid-configuration")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.userinfo_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.scopes_supported").isArray())
                .andExpect(jsonPath("$.response_types_supported").isArray())
                .andExpect(jsonPath("$.subject_types_supported").isArray())
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported").isArray())
                .andExpect(jsonPath("$.claims_supported").isArray());
    }

    @Test
    @DisplayName("测试获取 JWKS")
    @Transactional
    public void testGetJwks() throws Exception {
        mockMvc.perform(get("/api/oauth2/jwks")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").exists())
                .andExpect(jsonPath("$.keys").isArray());
    }

    @Test
    @DisplayName("未授权访问用户信息端点")
    @Transactional
    public void testUserInfoUnauthorized() throws Exception {
        mockMvc.perform(get("/api/oauth2/userinfo")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("使用无效的访问令牌访问用户信息端点")
    @Transactional
    public void testUserInfoWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/oauth2/userinfo")
                .header("Authorization", "Bearer invalid_token")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("刷新令牌后访问用户信息端点")
    @Transactional
    public void testUserInfoAfterTokenRefresh() throws Exception {
        // 先刷新令牌
        refreshAccessToken();
        
        // 使用新令牌访问用户信息
        mockMvc.perform(get("/api/oauth2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").exists())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }
} 