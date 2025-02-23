package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.TokenRevokeRequest;
import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.base.enums.AuthErrorCode;
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
import com.double2and9.auth_service.security.JwtTokenProvider;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
    private JwtTokenProvider jwtTokenProvider;

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

    @Test
    void revokeToken_RevokedRefreshToken() throws Exception {
        // 创建新的授权码
        AuthorizationCode newAuthCode = new AuthorizationCode();
        newAuthCode.setCode("test_code_2");  // 使用新的授权码
        newAuthCode.setClientId("test_client");
        newAuthCode.setUserId("test_user");
        newAuthCode.setRedirectUri("http://localhost:8080/callback");
        newAuthCode.setScope("read write");
        newAuthCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        newAuthCode.setUsed(false);
        authorizationCodeRepository.save(newAuthCode);

        // 获取新的令牌
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setGrantType("authorization_code");
        tokenRequest.setCode("test_code_2");  // 使用新的授权码
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
        String refreshToken = tokenResponse.getRefreshToken();

        // 撤销刷新令牌
        TokenRevokeRequest revokeRequest = new TokenRevokeRequest();
        revokeRequest.setToken(refreshToken);
        revokeRequest.setTokenTypeHint("refresh_token");

        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revokeRequest)))
                .andExpect(status().isOk());

        // 尝试使用已撤销的刷新令牌获取新的访问令牌
        TokenRequest refreshRequest = new TokenRequest();
        refreshRequest.setGrantType("refresh_token");
        refreshRequest.setClientId("test_client");
        refreshRequest.setClientSecret("test_secret");
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_REVOKED.getCode()))
                .andExpect(jsonPath("$.message").value("Token已被撤销"));
    }

    @Test
    void revokeToken_ExpiredToken() throws Exception {
        // 创建一个已过期的令牌
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "test_user");
        claims.put("clientId", "test_client");
        claims.put("scope", "read write");
        claims.put("type", "access_token");
        claims.put("iat", now - 7200);  // 2小时前签发
        claims.put("exp", now - 3600);  // 1小时前过期

        String expiredToken = jwtTokenProvider.generateToken(claims, 0);

        request.setToken(expiredToken);
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_INVALID.getCode()));
    }

    @Test
    void revokeToken_MalformedToken() throws Exception {
        request.setToken("malformed.token.structure");
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_INVALID.getCode()));
    }
} 