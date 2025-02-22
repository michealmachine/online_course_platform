package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import com.double2and9.auth_service.dto.response.TokenResponse;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TokenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthorizationCodeRepository authorizationCodeRepository;

    @Autowired
    private RegisteredClientRepository clientRepository;

    private TokenRequest request;
    private AuthorizationCode authCode;
    private RegisteredClient client;

    @BeforeEach
    void setUp() {
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

        // 创建授权码
        authCode = new AuthorizationCode();
        authCode.setCode("test_code");
        authCode.setClientId("test_client");
        authCode.setUserId("test_user");
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("read write");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        
        authorizationCodeRepository.save(authCode);

        // 设置请求数据
        request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setCode("test_code");
        request.setRedirectUri("http://localhost:8080/callback");
        request.setClientId("test_client");
        request.setClientSecret("test_secret");
    }

    @Test
    void token_Success() throws Exception {
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.scope").exists());
    }

    @Test
    void token_InvalidGrantType() throws Exception {
        request.setGrantType("invalid_grant_type");

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_GRANT_TYPE.getCode()));
    }

    @Test
    void token_InvalidClient() throws Exception {
        request.setClientId("invalid_client");

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_CLIENT_CREDENTIALS.getCode()));
    }

    @Test
    void token_InvalidCode() throws Exception {
        request.setCode("invalid_code");

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_AUTHORIZATION_CODE.getCode()));
    }

    @Test
    void refreshToken_Success() throws Exception {
        // 先获取访问令牌和刷新令牌
        MvcResult result = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            TokenResponse.class
        );

        // 使用刷新令牌
        TokenRequest refreshRequest = new TokenRequest();
        refreshRequest.setGrantType("refresh_token");
        refreshRequest.setRefreshToken(tokenResponse.getRefreshToken());
        refreshRequest.setClientId("test_client");
        refreshRequest.setClientSecret("test_secret");

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.scope").exists());
    }
} 