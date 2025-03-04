package com.double2and9.auth_service.integration;

import com.double2and9.base.enums.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OAuth2 令牌操作的集成测试
 * 测试令牌刷新、令牌撤销和令牌内省等功能
 */
public class TokenOperationIntegrationTest extends BaseOAuth2IntegrationTest {

    @Test
    @DisplayName("测试刷新令牌")
    @Transactional
    public void testRefreshToken() throws Exception {
        mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").exists());
    }

    @Test
    @DisplayName("测试使用无效的刷新令牌")
    @Transactional
    public void testInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", "invalid_refresh_token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试令牌内省 - 有效令牌")
    @Transactional
    public void testTokenIntrospection() throws Exception {
        mockMvc.perform(post("/api/oauth2/introspect")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", accessToken)
                .param("token_type_hint", "access_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.client_id").value(firstPartyClient.getClientId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.scope").exists())
                .andExpect(jsonPath("$.exp").exists())
                .andExpect(jsonPath("$.iat").exists());
    }

    @Test
    @DisplayName("测试令牌内省 - 无效令牌")
    @Transactional
    public void testInvalidTokenIntrospection() throws Exception {
        mockMvc.perform(post("/api/oauth2/introspect")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "invalid_token")
                .param("token_type_hint", "access_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("测试令牌撤销")
    @Transactional
    public void testTokenRevocation() throws Exception {
        // 首先撤销令牌
        mockMvc.perform(post("/api/oauth2/revoke")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", accessToken)
                .param("token_type_hint", "access_token"))
                .andExpect(status().isOk());

        // 然后尝试使用已撤销的令牌访问受保护资源
        mockMvc.perform(get("/api/oauth2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_REVOKED.getCode()));

        // 最后验证令牌内省结果显示令牌已失效
        mockMvc.perform(post("/api/oauth2/introspect")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", accessToken)
                .param("token_type_hint", "access_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("测试无权限时的令牌内省")
    @Transactional
    public void testTokenIntrospectionWithoutAuthorization() throws Exception {
        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", accessToken)
                .param("token_type_hint", "access_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("测试无权限时的令牌撤销")
    @Transactional
    public void testTokenRevocationWithoutAuthorization() throws Exception {
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", accessToken)
                .param("token_type_hint", "access_token"))
                .andExpect(status().isUnauthorized());
    }
} 