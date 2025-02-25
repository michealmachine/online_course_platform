package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
import com.double2and9.auth_service.security.JwtTokenProvider;
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
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.service.JwtService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
        // 不再设置clientId和clientSecret - 将使用HTTP Basic认证或专门测试旧的API
    }

    @Test
    void token_Success() throws Exception {
        // 使用HTTP Basic认证
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
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
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_GRANT_TYPE.getCode()));
    }

    @Test
    void token_InvalidClient() throws Exception {
        // 使用错误的客户端凭据
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("invalid_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_CLIENT_CREDENTIALS.getCode()));
    }

    @Test
    void token_InvalidCode() throws Exception {
        request.setCode("invalid_code");

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_AUTHORIZATION_CODE.getCode()));
    }

    @Test
    void refreshToken_Success() throws Exception {
        // 先获取访问令牌和刷新令牌
        MvcResult result = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
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

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.scope").value("read write"));
    }

    @Test
    void refreshToken_InvalidRefreshToken() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setGrantType("refresh_token");
        request.setRefreshToken("invalid.refresh.token");

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("无效的Token"));
    }

    @Test
    void refreshToken_InvalidClientCredentials() throws Exception {
        // 先获取有效的刷新令牌
        MvcResult result = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            TokenResponse.class
        );

        // 使用错误的客户端凭证
        TokenRequest refreshRequest = new TokenRequest();
        refreshRequest.setGrantType("refresh_token");
        refreshRequest.setRefreshToken(tokenResponse.getRefreshToken());

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("invalid_client:invalid_secret".getBytes()))
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_CLIENT_CREDENTIALS.getCode()))
                .andExpect(jsonPath("$.message").value("客户端认证失败"));
    }

    @Test
    void refreshToken_ExpiredRefreshToken() throws Exception {
        // 创建一个已过期的刷新令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "test_user");
        claims.put("clientId", "test_client");
        claims.put("scope", "read write");
        claims.put("type", "refresh_token");
        // 设置过期时间为1小时之前
        claims.put("exp", System.currentTimeMillis() / 1000 - 3600);
        claims.put("iat", System.currentTimeMillis() / 1000 - 7200);
        String expiredToken = jwtTokenProvider.generateToken(claims, -3600L); // 负数表示已过期

        TokenRequest refreshRequest = new TokenRequest();
        refreshRequest.setGrantType("refresh_token");
        refreshRequest.setRefreshToken(expiredToken);
        // 不再设置clientId和clientSecret，改用HTTP Basic认证

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_EXPIRED.getCode()))
                .andExpect(jsonPath("$.message").value("Token已过期"));
    }

    @Test
    void refreshToken_RevokedToken() throws Exception {
        // 先获取有效的刷新令牌
        MvcResult result = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            TokenResponse.class
        );

        // 撤销令牌
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", tokenResponse.getRefreshToken()))))
                .andExpect(status().isOk());

        // 尝试使用已撤销的令牌
        TokenRequest refreshRequest = new TokenRequest();
        refreshRequest.setGrantType("refresh_token");
        refreshRequest.setRefreshToken(tokenResponse.getRefreshToken());
        // 不再设置clientId和clientSecret，改用HTTP Basic认证

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_REVOKED.getCode()))
                .andExpect(jsonPath("$.message").value("Token已被撤销"));
    }
    
    // 新增测试方法：使用HTTP Basic认证
    @Test
    void token_BasicAuth_Success() throws Exception {
        // 创建令牌请求
        TokenRequest request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setCode("test_code");
        request.setRedirectUri("http://localhost:8080/callback");
        // 不再设置clientId和clientSecret，而是使用HTTP Basic认证头

        // 执行请求
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.scope").value("read write"));
    }
    
    @Test
    void token_BasicAuth_Invalid() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setCode("test_code");
        request.setRedirectUri("http://localhost:8080/callback");

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("invalid_client:invalid_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(301002))
                .andExpect(jsonPath("$.message").value("客户端认证失败"));
    }
    
    @Test
    void refreshToken_BasicAuth_Success() throws Exception {
        // 先获取授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("refresh_test_code");
        authCode.setClientId("test_client");
        authCode.setUserId("test_user");
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("read write");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        authorizationCodeRepository.save(authCode);
        
        // 先使用授权码获取有效的令牌和刷新令牌
        TokenRequest initialTokenRequest = new TokenRequest();
        initialTokenRequest.setGrantType("authorization_code");
        initialTokenRequest.setCode("refresh_test_code");
        initialTokenRequest.setRedirectUri("http://localhost:8080/callback");
        
        MvcResult initialResult = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(initialTokenRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        TokenResponse initialTokenResponse = objectMapper.readValue(initialResult.getResponse().getContentAsString(), TokenResponse.class);
        String refreshToken = initialTokenResponse.getRefreshToken();
        
        // 确保刷新令牌不为空
        assertNotNull(refreshToken, "Refresh token should not be null");
        
        // 使用刷新令牌获取新的访问令牌
        TokenRequest refreshTokenRequest = new TokenRequest();
        refreshTokenRequest.setGrantType("refresh_token");
        refreshTokenRequest.setRefreshToken(refreshToken);
        
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.scope").value("read write"));
    }
} 