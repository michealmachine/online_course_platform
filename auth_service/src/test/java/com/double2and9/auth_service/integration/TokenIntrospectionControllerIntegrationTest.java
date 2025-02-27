package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.TokenIntrospectionRequest;
import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.service.TokenBlacklistService;
import com.double2and9.base.enums.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TokenIntrospectionControllerIntegrationTest {

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
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private String validToken;
    private String validRefreshToken;
    private String savedUserId;

    @BeforeEach
    void setUp() throws Exception {
        // 清除Redis数据
        redisConnectionFactory.getConnection().serverCommands().flushDb();

        // 创建测试客户端
        RegisteredClient client = RegisteredClient.withId("1")
            .clientId("test_client")
            .clientSecret("test_secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .redirectUri("http://localhost:8080/callback")
            .scope("read")
            .scope("write")
            .build();
        clientRepository.save(client);

        // 创建测试用户
        User testUser = new User();
        testUser.setUsername("test_user");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setPassword("test_password");
        testUser.setEmailVerified(true);
        testUser.setPhoneVerified(false);
        User savedUser = userRepository.save(testUser);
        savedUserId = savedUser.getId().toString();

        // 创建授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("test_code");
        authCode.setClientId("test_client");
        authCode.setUserId(savedUserId);
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("read write");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        authorizationCodeRepository.save(authCode);

        // 获取有效的令牌
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
        validRefreshToken = tokenResponse.getRefreshToken();

        // 验证获取到的令牌
        assertNotNull(validToken, "Access token should not be null");
        assertNotNull(validRefreshToken, "Refresh token should not be null");
    }

    @AfterEach
    void tearDown() {
        // 清除Redis数据
        redisConnectionFactory.getConnection().serverCommands().flushDb();
    }

    @Test
    void introspect_ValidAccessToken() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(validToken);
        request.setTokenTypeHint("access_token");

        // 打印令牌信息用于调试
        log.debug("Testing token introspection with token: {}", validToken);

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.clientId").value("test_client"))
                .andExpect(jsonPath("$.userId").value(savedUserId))
                .andExpect(jsonPath("$.scope").value("read write"))
                .andExpect(jsonPath("$.tokenType").value("access_token"))
                .andExpect(jsonPath("$.exp").isNumber())
                .andExpect(jsonPath("$.iat").isNumber())
                .andDo(result -> {
                    // 打印响应内容用于调试
                    log.debug("Introspection response: {}", result.getResponse().getContentAsString());
                });
    }

    @Test
    void introspect_ValidRefreshToken() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(validRefreshToken);
        request.setTokenTypeHint("refresh_token");

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.clientId").value("test_client"))
                .andExpect(jsonPath("$.userId").value(savedUserId))
                .andExpect(jsonPath("$.scope").value("read write"))
                .andExpect(jsonPath("$.tokenType").value("refresh_token"));
    }

    @Test
    void introspect_InvalidToken() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("invalid.jwt.token");

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void introspect_EmptyToken() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("");

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void introspect_RevokedToken() throws Exception {
        // 先撤销令牌
        mockMvc.perform(post("/api/oauth2/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(Map.of("token", validToken))))
                .andExpect(status().isOk());

        // 然后尝试内省
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(validToken);

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void introspect_ExpiredToken() throws Exception {
        // 等待令牌过期（这里我们使用一个已经过期的令牌）
        String expiredToken = "eyJhbGciOiJIUzUxMiJ9.eyJleHAiOjE2NDA5OTUyMDAsImlhdCI6MTY0MDk5MTYwMH0.expired_token";
        
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(expiredToken);

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void introspect_MalformedToken() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("malformed.token.structure");
        request.setTokenTypeHint("access_token");

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void introspect_WrongTokenTypeHint() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(validToken);
        request.setTokenTypeHint("wrong_type");

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.clientId").value("test_client"))
                .andExpect(jsonPath("$.userId").value(savedUserId))
                .andExpect(jsonPath("$.tokenType").exists());
    }

    @Test
    void introspect_NoTokenTypeHint() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(validToken);
        // 不设置 tokenTypeHint

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.clientId").value("test_client"))
                .andExpect(jsonPath("$.userId").value(savedUserId))
                .andExpect(jsonPath("$.tokenType").exists());
    }

    @Test
    void introspect_RefreshTokenWithAccessTokenHint() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(validRefreshToken);
        request.setTokenTypeHint("access_token");

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.clientId").value("test_client"))
                .andExpect(jsonPath("$.userId").value(savedUserId))
                .andExpect(jsonPath("$.tokenType").value("refresh_token"));
    }

    @Test
    void introspect_ValidIdToken() throws Exception {
        // 创建带有openid scope的授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("openid_test_code");
        authCode.setClientId("test_client");
        authCode.setUserId(savedUserId);
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("openid profile email");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        authorizationCodeRepository.save(authCode);

        // 获取ID Token
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setGrantType("authorization_code");
        tokenRequest.setCode("openid_test_code");
        tokenRequest.setRedirectUri("http://localhost:8080/callback");
        tokenRequest.setNonce("test_nonce");

        MvcResult tokenResult = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
            tokenResult.getResponse().getContentAsString(),
            TokenResponse.class
        );

        // 验证获取到的ID Token不为空
        assertNotNull(tokenResponse.getIdToken(), "ID Token should not be null");
        log.debug("Generated ID Token: {}", tokenResponse.getIdToken());

        // 内省ID Token
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(tokenResponse.getIdToken());
        request.setTokenTypeHint("id_token");

        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").value(savedUserId))  // 使用userId而不是sub
                .andExpect(jsonPath("$.clientId").value("test_client"))  // 使用clientId而不是aud
                .andExpect(jsonPath("$.scope").value("openid profile email"))
                .andExpect(jsonPath("$.tokenType").value("id_token"))
                .andExpect(jsonPath("$.exp").isNumber())
                .andExpect(jsonPath("$.iat").isNumber())
                .andDo(result -> {
                    // 打印响应内容用于调试
                    log.debug("Introspection response: {}", result.getResponse().getContentAsString());
                });
    }

    @Test
    void introspect_InvalidClientAuth() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken(validToken);

        // 使用无效的Basic认证头格式来触发认证异常
        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic invalid_auth_header")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(301002))
                .andExpect(jsonPath("$.message").value("客户端认证失败"));
    }
} 