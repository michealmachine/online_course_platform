package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.TokenRevokeRequest;
import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import java.time.LocalDateTime;

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

    private TokenRevokeRequest request;
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

        // 先获取一个有效的令牌
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setGrantType("authorization_code");
        tokenRequest.setCode("test_code");
        tokenRequest.setRedirectUri("http://localhost:8080/callback");
        tokenRequest.setClientId("test_client");
        tokenRequest.setClientSecret("test_secret");

        String response = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TokenResponse tokenResponse = objectMapper.readValue(response, TokenResponse.class);
        validToken = tokenResponse.getAccessToken();

        // 设置撤销请求
        request = new TokenRevokeRequest();
        request.setToken(validToken);
        request.setTokenTypeHint("access_token");
    }

    @Test
    void revokeToken_Success() throws Exception {
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void revokeToken_InvalidToken() throws Exception {
        request.setToken("invalid.token");
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokeToken_EmptyToken() throws Exception {
        request.setToken("");
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(300501))
                .andExpect(jsonPath("$.message").value("参数验证失败"));
    }
} 