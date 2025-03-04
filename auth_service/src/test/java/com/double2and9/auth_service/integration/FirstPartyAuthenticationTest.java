package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.AuthServiceApplication;
import com.double2and9.auth_service.config.BaseIntegrationTestConfig;
import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.utils.PKCEUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@Import(BaseIntegrationTestConfig.class)
@ActiveProfiles("dev")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class FirstPartyAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomJdbcRegisteredClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.client.web.secret:web-client-secret}")
    private String webClientSecret;

    private Role userRole;
    private User testUser;
    private RegisteredClient firstPartyClient;
    private String codeVerifier;
    private String codeChallenge;
    private String state;
    private String nonce;
    private MockHttpSession session;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        // 清理测试数据
        userRepository.deleteAll();
        
        // 初始化会话
        session = new MockHttpSession();

        // 生成 PKCE 参数
        generatePkceParameters();

        // 验证 PKCE 参数是否正确生成
        assertTrue(PKCEUtils.verifyCodeChallenge(codeVerifier, codeChallenge, "S256"),
            "PKCE verification failed");

        // 确保有USER角色
        userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    role.setDescription("普通用户角色");
                    return roleRepository.save(role);
                });

        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setRoles(Collections.singleton(userRole));
        
        // 设置 OIDC 相关字段
        testUser.setPreferredUsername("testuser");
        testUser.setGivenName("Test");
        testUser.setFamilyName("User");
        testUser.setEmailVerified(false);
        testUser.setPhoneVerified(false);
        
        testUser = userRepository.save(testUser);

        // 验证用户是否成功保存
        User savedUser = userRepository.findByUsername("testuser")
                .orElseThrow(() -> new RuntimeException("Failed to save test user"));
        
        // 验证用户信息
        assert savedUser.getUsername().equals("testuser");
        assert savedUser.getEmail().equals("test@example.com");
        assert savedUser.getRoles().contains(userRole);

        // 使用预置的web-client
        RegisteredClient client = clientRepository.findByClientId("web-client");
        if (client == null) {
            throw new RuntimeException("Web client not found");
        }
        firstPartyClient = client;
    }

    private void generatePkceParameters() throws Exception {
        // 生成 code verifier
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifierBytes = new byte[32];
        secureRandom.nextBytes(codeVerifierBytes);
        codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes);

        // 生成 code challenge
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // 生成 state 和 nonce
        byte[] stateBytes = new byte[16];
        secureRandom.nextBytes(stateBytes);
        state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);

        byte[] nonceBytes = new byte[16];
        secureRandom.nextBytes(nonceBytes);
        nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
    }

    @Test
    @Transactional
    void testCompleteOAuth2Flow() throws Exception {
        // 验证测试用户存在
        User currentUser = userRepository.findByUsername("testuser")
                .orElseThrow(() -> new RuntimeException("Test user not found before starting the test"));
        System.out.println("Test user found: " + currentUser.getUsername());
        System.out.println("Test user ID: " + currentUser.getId());
        System.out.println("Test user roles: " + currentUser.getRoles());

        // 确保用户数据已经提交到数据库
        userRepository.flush();

        // 1. 提交登录请求
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 从响应体中获取认证token
        Map<String, Object> response = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(),
            Map.class
        );
        String authToken = (String) response.get("token");
        assertNotNull(authToken, "Authorization token should not be null");

        // 2. 使用认证token请求授权
        System.out.println("Using auth token: " + authToken);
        
        // 再次验证用户存在
        User userBeforeAuthorize = userRepository.findByUsername("testuser")
                .orElseThrow(() -> new RuntimeException("Test user not found before authorization"));
        System.out.println("User still exists before authorize: " + userBeforeAuthorize.getUsername());
        System.out.println("User ID before authorize: " + userBeforeAuthorize.getId());

        MvcResult authorizeResult = mockMvc.perform(get("/api/oauth2/authorize")
                .header("Authorization", "Bearer " + authToken)
                .session(session)
                .param("response_type", "code")
                .param("client_id", firstPartyClient.getClientId())
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("scope", "openid profile email")
                .param("state", state)
                .param("nonce", nonce)
                .param("code_challenge", codeChallenge)
                .param("code_challenge_method", "S256"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(firstPartyClient.getClientId()))
                .andExpect(jsonPath("$.authorizationCode").exists())
                .andReturn();

        String authorizationCode = objectMapper.readTree(authorizeResult.getResponse().getContentAsString())
                .get("authorizationCode").asText();
        assertNotNull(authorizationCode, "Authorization code should not be null");

        // 3. 使用授权码交换令牌
        MvcResult tokenResult = mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("code_verifier", codeVerifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                tokenResult.getResponse().getContentAsString(),
                TokenResponse.class
        );
        
        assertNotNull(tokenResponse.getAccessToken(), "Access token should not be null");
        assertNotNull(tokenResponse.getRefreshToken(), "Refresh token should not be null");
        assertNotNull(tokenResponse.getIdToken(), "ID token should not be null");
        assertEquals("Bearer", tokenResponse.getTokenType(), "Token type should be Bearer");

        // 4. 使用访问令牌获取用户信息
        mockMvc.perform(get("/api/oauth2/userinfo")
                .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").exists())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.preferred_username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email_verified").exists());
    }

    @Test
    @Transactional
    void testLoginWithInvalidCredentials() throws Exception {
        // 提交无效的登录请求
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(300104));
    }

    @Test
    @Transactional
    void testInvalidPkceParameters() throws Exception {
        // 1. 先进行正常登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(),
            Map.class
        );
        String authToken = (String) response.get("token");

        // 2. 使用无效的 code_challenge 请求授权
        mockMvc.perform(get("/api/oauth2/authorize")
                .header("Authorization", "Bearer " + authToken)
                .session(session)
                .param("response_type", "code")
                .param("client_id", firstPartyClient.getClientId())
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("scope", "openid profile email")
                .param("state", state)
                .param("nonce", nonce)
                .param("code_challenge", "invalid_challenge")
                .param("code_challenge_method", "S256"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(301102)); // 期望 INVALID_CODE_CHALLENGE 错误码
    }

    private String extractCode(String redirectUrl) {
        int codeStart = redirectUrl.indexOf("code=") + 5;
        int codeEnd = redirectUrl.indexOf("&", codeStart);
        if (codeEnd == -1) {
            codeEnd = redirectUrl.length();
        }
        return redirectUrl.substring(codeStart, codeEnd);
    }
}
