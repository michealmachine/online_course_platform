package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.base.enums.AuthErrorCode;
import com.double2and9.base.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
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
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import com.double2and9.auth_service.service.JwtService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private TokenRequest request;
    private AuthorizationCode authCode;
    private RegisteredClient client;
    private User savedUser;

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

        // 获取默认角色
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        // 创建测试用户
        User testUser = new User();
        testUser.setUsername("test_user_" + System.currentTimeMillis());  // 使用时间戳确保唯一
        testUser.setEmail("test_" + System.currentTimeMillis() + "@example.com");
        testUser.setEnabled(true);
        testUser.setPassword("test_password");
        testUser.setEmailVerified(true);
        testUser.setPhoneVerified(false);
        testUser.setGivenName("Test");
        testUser.setFamilyName("User");
        testUser.setPhone("1234567890");
        testUser.setRoles(Collections.singleton(userRole));
        savedUser = userRepository.save(testUser);

        // 创建授权码
        authCode = new AuthorizationCode();
        authCode.setCode("test_code");
        authCode.setClientId("test_client");
        authCode.setUserId(String.valueOf(savedUser.getId()));  // 使用保存后的用户ID
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
    }

    @Test
    void token_Success() throws Exception {
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
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
        // 使用无效的Basic认证头格式来触发认证异常
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic invalid_auth_header")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_CLIENT_CREDENTIALS.getCode()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.INVALID_CLIENT_CREDENTIALS.getMessage()));
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
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
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
                .andExpect(jsonPath("$.message").value(AuthErrorCode.TOKEN_INVALID.getMessage()));
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
        claims.put("userId", String.valueOf(savedUser.getId()));  // 使用savedUser的ID
        claims.put("clientId", "test_client");
        claims.put("scope", "read write");
        claims.put("type", "refresh_token");
        // 设置过期时间为1小时之前
        claims.put("exp", System.currentTimeMillis() / 1000 - 3600);
        claims.put("iat", System.currentTimeMillis() / 1000 - 7200);
        String expiredToken = jwtTokenProvider.generateToken(claims, -3600L);

        TokenRequest refreshRequest = new TokenRequest();
        refreshRequest.setGrantType("refresh_token");
        refreshRequest.setRefreshToken(expiredToken);

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.TOKEN_INVALID.getMessage()));
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
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(Map.of("token", tokenResponse.getRefreshToken()))))
                .andExpect(status().isOk());

        // 尝试使用已撤销的令牌
        TokenRequest refreshRequest = new TokenRequest();
        refreshRequest.setGrantType("refresh_token");
        refreshRequest.setRefreshToken(tokenResponse.getRefreshToken());

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

        // 执行请求
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
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
        // 创建授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("refresh_test_code");
        authCode.setClientId("test_client");
        authCode.setUserId(String.valueOf(savedUser.getId()));  // 使用实际保存的用户ID
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
        
        TokenResponse initialTokenResponse = objectMapper.readValue(
            initialResult.getResponse().getContentAsString(),
            TokenResponse.class
        );
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
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.scope").value("read write"));
    }

    @Test
    void token_WithOpenIdScope_Success() throws Exception {
        // 创建带有openid scope的授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("openid_test_code");
        authCode.setClientId("test_client");
        authCode.setUserId(String.valueOf(savedUser.getId()));  // 使用实际保存的用户ID
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("openid profile email");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        authorizationCodeRepository.save(authCode);

        // 创建token请求
        TokenRequest request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setCode("openid_test_code");
        request.setRedirectUri("http://localhost:8080/callback");
        request.setNonce("test_nonce");

        // 执行请求
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.scope").value("openid profile email"));
    }

    @Test
    void token_WithoutOpenIdScope_NoIdToken() throws Exception {
        // 获取默认角色
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        // 创建测试用户
        User testUser = new User();
        testUser.setUsername("test_user_no_openid_" + System.currentTimeMillis());  // 使用时间戳确保唯一
        testUser.setEmail("test_no_openid_" + System.currentTimeMillis() + "@example.com");
        testUser.setEnabled(true);
        testUser.setPassword("test_password");  // 设置密码
        testUser.setRoles(Collections.singleton(userRole));
        User savedUser = userRepository.save(testUser);

        // 创建不带openid scope的授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("profile_test_code");
        authCode.setClientId("test_client");
        authCode.setUserId(String.valueOf(savedUser.getId()));  // 使用保存后的用户ID
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("profile email");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        authorizationCodeRepository.save(authCode);

        // 创建token请求
        TokenRequest request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setCode("profile_test_code");
        request.setRedirectUri("http://localhost:8080/callback");

        // 执行请求
        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.id_token").doesNotExist())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.scope").value("profile email"));
    }

    @Test
    void refreshToken_WithOpenIdScope_Success() throws Exception {
        // 创建带有openid scope的授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("openid_refresh_test");
        authCode.setClientId("test_client");
        authCode.setUserId(String.valueOf(savedUser.getId()));  // 使用实际保存的用户ID
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("openid profile email");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        authorizationCodeRepository.save(authCode);

        // 获取初始token
        TokenRequest initialRequest = new TokenRequest();
        initialRequest.setGrantType("authorization_code");
        initialRequest.setCode("openid_refresh_test");
        initialRequest.setRedirectUri("http://localhost:8080/callback");
        initialRequest.setNonce("initial_nonce");

        MvcResult initialResult = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(initialRequest)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse initialResponse = objectMapper.readValue(
            initialResult.getResponse().getContentAsString(),
            TokenResponse.class
        );

        // 使用刷新令牌
        TokenRequest refreshRequest = new TokenRequest();
        refreshRequest.setGrantType("refresh_token");
        refreshRequest.setRefreshToken(initialResponse.getRefreshToken());
        refreshRequest.setNonce("refresh_nonce");
        refreshRequest.setScope("openid profile email");  // 添加scope参数

        mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.scope").value("openid profile email"));
    }

    @Test
    void token_WithOpenIdScope_ValidateIdTokenClaims() throws Exception {
        // 获取默认角色
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        // 创建测试用户
        User testUser = new User();
        String timestamp = String.valueOf(System.currentTimeMillis());
        testUser.setUsername("test_user_validate_" + timestamp);
        testUser.setEmail("test_validate_" + timestamp + "@example.com");
        testUser.setEmailVerified(true);
        testUser.setGivenName("Test");
        testUser.setFamilyName("User");
        testUser.setPhone("1234567890");
        testUser.setPhoneVerified(true);
        testUser.setPassword("test_password");
        testUser.setEnabled(true);
        testUser.setRoles(Collections.singleton(userRole));
        User savedUser = userRepository.save(testUser);

        // 创建带有openid scope的授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("openid_test_code");
        authCode.setClientId("test_client");
        authCode.setUserId(String.valueOf(savedUser.getId()));
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("openid profile email phone");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        authorizationCodeRepository.save(authCode);

        // 创建token请求
        TokenRequest request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setCode("openid_test_code");
        request.setRedirectUri("http://localhost:8080/callback");
        request.setNonce("test_nonce");

        // 执行请求
        MvcResult result = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andReturn();

        // 解析响应
        TokenResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            TokenResponse.class
        );

        // 验证ID Token的claims
        Claims claims = jwtService.parseToken(response.getIdToken());
        assertEquals(String.valueOf(savedUser.getId()), claims.get("sub"));
        assertEquals("test_client", claims.get("aud"));
        assertEquals("test_nonce", claims.get("nonce"));
        assertEquals("Test", claims.get("given_name"));
        assertEquals("User", claims.get("family_name"));
        assertEquals(testUser.getEmail(), claims.get("email"));
        assertTrue((Boolean) claims.get("email_verified"));
        assertEquals("1234567890", claims.get("phone_number"));
        assertTrue((Boolean) claims.get("phone_number_verified"));

        // 验证ID Token的内省结果
        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(Map.of(
                    "token", response.getIdToken(),
                    "tokenTypeHint", "id_token"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").value(String.valueOf(savedUser.getId())))
                .andExpect(jsonPath("$.clientId").value("test_client"))
                .andExpect(jsonPath("$.scope").value("openid profile email"))
                .andExpect(jsonPath("$.tokenType").value("id_token"))
                .andExpect(jsonPath("$.exp").isNumber())
                .andExpect(jsonPath("$.iat").isNumber());
    }

    @Test
    void introspect_ValidIdToken() throws Exception {
        // 创建测试用户
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test_user_introspect_" + System.currentTimeMillis());  // 使用时间戳确保唯一
        testUser.setEmail("test_introspect_" + System.currentTimeMillis() + "@example.com");
        testUser.setEmailVerified(true);
        testUser.setPassword("test_password");  // 设置密码
        testUser.setEnabled(true);
        testUser.setRoles(Collections.singleton(userRole));
        userRepository.save(testUser);

        // 创建带有openid scope的授权码
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode("openid_introspect_test");
        authCode.setClientId("test_client");
        authCode.setUserId(String.valueOf(savedUser.getId()));  // 使用实际保存的用户ID
        authCode.setRedirectUri("http://localhost:8080/callback");
        authCode.setScope("openid profile email");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setUsed(false);
        authorizationCodeRepository.save(authCode);

        // 获取ID Token
        TokenRequest request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setCode("openid_introspect_test");
        request.setRedirectUri("http://localhost:8080/callback");
        request.setNonce("test_nonce");

        MvcResult result = mockMvc.perform(post("/api/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            TokenResponse.class
        );

        // 内省ID Token
        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(Map.of(
                    "token", response.getIdToken(),
                    "tokenTypeHint", "id_token"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").value(String.valueOf(savedUser.getId())))
                .andExpect(jsonPath("$.clientId").value("test_client"))
                .andExpect(jsonPath("$.scope").value("openid profile email"))
                .andExpect(jsonPath("$.tokenType").value("id_token"))
                .andExpect(jsonPath("$.exp").isNumber())
                .andExpect(jsonPath("$.iat").isNumber());
    }

    @Test
    void introspect_ExpiredIdToken() throws Exception {
        // 创建一个已过期的ID Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", String.valueOf(savedUser.getId()));
        claims.put("aud", "test_client");
        claims.put("nonce", "test_nonce");
        claims.put("email", "test@example.com");
        // 设置过期时间为1小时之前
        claims.put("exp", System.currentTimeMillis() / 1000 - 3600);
        claims.put("iat", System.currentTimeMillis() / 1000 - 7200);
        String expiredToken = jwtTokenProvider.generateToken(claims, -3600L); // 负数表示已过期

        // 内省过期的ID Token
        mockMvc.perform(post("/api/oauth2/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test_client:test_secret".getBytes()))
                .content(objectMapper.writeValueAsString(Map.of(
                    "token", expiredToken,
                    "tokenTypeHint", "id_token"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
} 