package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.service.AuthorizationCodeService;
import com.double2and9.auth_service.utils.PKCEUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OAuth2 授权流程的集成测试
 * 专门测试 OAuth2 授权流程的各个环节
 */
@Slf4j
public class OAuth2AuthorizationIntegrationTest extends BaseOAuth2IntegrationTest {

    @Autowired
    private AuthorizationCodeService authorizationCodeService;

    @Test
    @DisplayName("测试完整的授权码流程")
    @Transactional
    public void testFullAuthorizationCodeFlow() throws Exception {
        MockHttpSession testSession = new MockHttpSession();
        
        // 第1步：登录获取认证令牌
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(testSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(),
            Map.class
        );
        String testAuthToken = (String) response.get("token");
        assertNotNull(testAuthToken, "认证令牌不应为空");
        
        // 第2步：请求授权码 - 使用从BaseOAuth2IntegrationTest中准备好的授权码
        log.info("使用预先准备好的授权码进行令牌交换，而不是重新请求授权码");
        String authorizationCode = authorizationCodeService.getLatestAuthorizationCode(testUser.getUsername(), firstPartyClient.getClientId());
        assertNotNull(authorizationCode, "预先准备的授权码不应为空");
        
        // 第3步：使用授权码交换令牌
        MvcResult tokenResult = mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .session(testSession)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("code_verifier", codeVerifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").exists())
                .andReturn();

        JsonNode tokenResponse = objectMapper.readTree(tokenResult.getResponse().getContentAsString());
        String testAccessToken = tokenResponse.get("access_token").asText();
        
        // 第4步：使用访问令牌访问受保护资源
        mockMvc.perform(get("/api/oauth2/userinfo")
                .header("Authorization", "Bearer " + testAccessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").exists())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    @DisplayName("测试PKCE验证")
    @Transactional
    public void testPKCEValidation() throws Exception {
        // 使用错误的code_verifier
        String wrongCodeVerifier = "wrong_code_verifier";
        
        // 使用已有的授权码发起令牌请求，但提供错误的code_verifier
        mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "valid_auth_code") // 这里实际上无关紧要，因为PKCE验证会先失败
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("code_verifier", wrongCodeVerifier))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试授权请求参数验证")
    @Transactional
    public void testAuthorizationRequestValidation() throws Exception {
        // 测试缺少必需参数 - 响应类型为必填参数
        mockMvc.perform(get("/api/oauth2/authorize")
                .header("Authorization", "Bearer " + authToken)
                .param("client_id", firstPartyClient.getClientId())
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("scope", "openid profile")
                // 缺少response_type参数
                )
                .andExpect(status().isBadRequest());
        
        // 测试无效的response_type
        mockMvc.perform(get("/api/oauth2/authorize")
                .header("Authorization", "Bearer " + authToken)
                .param("response_type", "invalid_type")
                .param("client_id", firstPartyClient.getClientId())
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("scope", "openid profile"))
                .andExpect(status().isBadRequest());
        
        // 测试无效的客户端ID
        mockMvc.perform(get("/api/oauth2/authorize")
                .header("Authorization", "Bearer " + authToken)
                .param("response_type", "code")
                .param("client_id", "invalid_client_id")
                .param("redirect_uri", "http://localhost:3000/callback"))
                .andExpect(status().isBadRequest());
        
        // 测试无效的重定向URI
        mockMvc.perform(get("/api/oauth2/authorize")
                .header("Authorization", "Bearer " + authToken)
                .param("response_type", "code")
                .param("client_id", firstPartyClient.getClientId())
                .param("redirect_uri", "http://malicious-site.com/callback"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试令牌请求参数验证")
    @Transactional
    public void testTokenRequestValidation() throws Exception {
        // 测试缺少必需参数
        mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                // 省略code和其他必需参数
                )
                .andExpect(status().isBadRequest());
        
        // 测试无效的grant_type
        mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "invalid_grant_type"))
                .andExpect(status().isBadRequest());
        
        // 测试无效的授权码
        mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "invalid_code")
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("code_verifier", codeVerifier))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("测试客户端身份验证")
    @Transactional
    public void testClientAuthentication() throws Exception {
        // 测试无效的客户端凭证
        mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), "wrong_secret"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "valid_auth_code") // 实际上验证不到这一步
                .param("redirect_uri", "http://localhost:3000/callback"))
                .andExpect(status().isUnauthorized());
        
        // 测试缺少客户端身份验证
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "valid_auth_code")
                .param("redirect_uri", "http://localhost:3000/callback"))
                .andExpect(status().isUnauthorized());
    }
} 