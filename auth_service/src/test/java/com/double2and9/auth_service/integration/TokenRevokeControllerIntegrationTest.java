package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.TokenRevokeRequest;
import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.request.TokenIntrospectionRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.base.enums.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TokenRevokeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomJdbcRegisteredClientRepository clientRepository;

    @Autowired
    private AuthorizationCodeRepository authorizationCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String validToken;
    private RegisteredClient client;
    private AuthorizationCode authCode;

    @BeforeEach
    void setUp() throws Exception {
        // 创建测试客户端
        client = RegisteredClient.withId("1")
                .clientId("test_client")
                .clientSecret("test_secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .redirectUri("http://localhost:8080/callback")
                .scope("read")
                .build();
        clientRepository.save(client);

        // 获取预置的ROLE_USER角色
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default ROLE_USER not found"));

        // 创建测试用户
        User testUser = new User();
        testUser.setUsername("test_user");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setPassword("test_password");
        testUser.setEmailVerified(true);
        testUser.setPhoneVerified(false);
        testUser.setRoles(Collections.singleton(userRole));
        User savedUser = userRepository.save(testUser);

        // 创建授权码
        authCode = new AuthorizationCode();
        authCode.setCode("test_code");
        authCode.setClientId("test_client");
        authCode.setUserId(savedUser.getId().toString());  // 使用保存后的用户ID
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("read write");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        
        authorizationCodeRepository.save(authCode);

        // 获取访问令牌
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setGrantType("authorization_code");
        tokenRequest.setCode("test_code");
        tokenRequest.setRedirectUri("http://localhost:8080/callback");

        MvcResult result = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
        validToken = tokenResponse.getAccessToken();
        assertNotNull(validToken, "Access token should not be null");
    }

    @Test
    void revokeToken_Success() throws Exception {
        TokenRevokeRequest request = new TokenRevokeRequest();
        request.setToken(validToken);

        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证令牌已被撤销
        TokenIntrospectionRequest introspectionRequest = new TokenIntrospectionRequest();
        introspectionRequest.setToken(validToken);

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(introspectionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void revokeToken_InvalidToken() throws Exception {
        TokenRevokeRequest request = new TokenRevokeRequest();
        request.setToken("invalid.token");

        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(300302))
                .andExpect(jsonPath("$.message").value("无效的Token"));
    }

    @Test
    void revokeToken_EmptyToken() throws Exception {
        TokenRevokeRequest request = new TokenRevokeRequest();
        request.setToken("");

        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revokeToken_NoClientAuth() throws Exception {
        TokenRevokeRequest request = new TokenRevokeRequest();
        request.setToken(validToken);

        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(301002))
                .andExpect(jsonPath("$.message").value("客户端认证失败"));
    }

    @Test
    void revokeToken_InvalidClientAuth() throws Exception {
        TokenRevokeRequest request = new TokenRevokeRequest();
        request.setToken(validToken);

        // 使用无效的Basic认证格式来触发认证异常
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic invalid_auth_header")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(301002))
                .andExpect(jsonPath("$.message").value("客户端认证失败"));
    }
} 