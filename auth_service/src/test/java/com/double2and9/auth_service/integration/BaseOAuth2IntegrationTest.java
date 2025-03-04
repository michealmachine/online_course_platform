package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.AuthServiceApplication;
import com.double2and9.auth_service.config.BaseIntegrationTestConfig;
import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.utils.PKCEUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.Arrays;
import java.util.UUID;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 基于OAuth2授权流程的集成测试基类
 * 提供获取访问令牌和调用保护资源的通用方法
 */
@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@Import(BaseIntegrationTestConfig.class)
@ActiveProfiles("dev")
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public abstract class BaseOAuth2IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected CustomJdbcRegisteredClientRepository clientRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Value("${app.client.web.secret:web-client-secret}")
    protected String webClientSecret;

    @Autowired
    protected RegisteredClientRepository registeredClientRepository;

    protected Role userRole;
    protected Role adminRole;
    protected User testUser;
    protected RegisteredClient firstPartyClient;
    protected String authToken;
    protected String accessToken;
    protected String refreshToken;
    protected String idToken;
    protected String codeVerifier;
    protected String codeChallenge;
    protected String state;
    protected String nonce;
    protected MockHttpSession session;

    @BeforeEach
    @Transactional
    public void setUp() throws Exception {
        // 初始化会话
        session = new MockHttpSession();

        // 生成 PKCE 参数
        generatePkceParameters();

        // 确保有USER角色
        userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    role.setDescription("普通用户角色");
                    return roleRepository.save(role);
                });

        // 添加ADMIN角色
        adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    role.setDescription("管理员角色");
                    return roleRepository.save(role);
                });

        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));  // 只赋予USER角色
        
        // 设置 OIDC 相关字段
        testUser.setPreferredUsername("testuser");
        testUser.setGivenName("Test");
        testUser.setFamilyName("User");
        testUser.setEmailVerified(false);
        testUser.setPhoneVerified(false);
        
        testUser = userRepository.save(testUser);

        // 使用预置的web-client
        RegisteredClient client = clientRepository.findByClientId("web-client");
        if (client == null) {
            throw new RuntimeException("Web client not found");
        }
        firstPartyClient = client;
        
        // 确保web-client被标记为内部客户端并允许自动授权
        log.info("检查web-client是否为内部客户端并允许自动授权");
        boolean isInternal = clientRepository.isInternalClient(firstPartyClient.getClientId());
        boolean isAutoApprove = clientRepository.isAutoApproveClient(firstPartyClient.getClientId());
        
        if (!isInternal || !isAutoApprove) {
            log.info("更新web-client为内部客户端并允许自动授权");
            jdbcTemplate.update(
                "UPDATE oauth2_registered_client SET is_internal = true, auto_approve = true WHERE client_id = ?",
                firstPartyClient.getClientId()
            );
        } else {
            log.info("web-client已经是内部客户端并允许自动授权");
        }

        // 获取管理员token
        setupAdminWithToken();
    }

    /**
     * 生成PKCE参数（Code Verifier和Code Challenge）
     */
    protected void generatePkceParameters() throws Exception {
        // 生成随机的code_verifier
        byte[] verifierBytes = new byte[32];
        new SecureRandom().nextBytes(verifierBytes);
        codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);
        
        // 计算code_challenge = BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] challengeBytes = md.digest(codeVerifier.getBytes("US-ASCII"));
        codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
        
        // 生成state和nonce
        byte[] stateBytes = new byte[16];
        new SecureRandom().nextBytes(stateBytes);
        state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
        
        byte[] nonceBytes = new byte[16];
        new SecureRandom().nextBytes(nonceBytes);
        nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        
        // 验证PKCE参数生成正确
        assertTrue(PKCEUtils.verifyCodeChallenge(codeVerifier, codeChallenge, "S256"));
    }

    /**
     * 完成完整的OAuth2授权流程，获取访问令牌
     */
    protected void completeOAuth2Flow() throws Exception {
        // 1. 登录请求获取认证令牌
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
        authToken = (String) response.get("token");
        
        // 2. 使用认证token请求授权码
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
                .andReturn();

        String authorizationCode = objectMapper.readTree(authorizeResult.getResponse().getContentAsString())
                .get("authorizationCode").asText();

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
                .andReturn();

        // 解析令牌响应
        Map<String, Object> tokenResponse = objectMapper.readValue(
                tokenResult.getResponse().getContentAsString(),
                Map.class
        );
        
        // 保存令牌信息供测试使用
        accessToken = (String) tokenResponse.get("access_token");
        refreshToken = (String) tokenResponse.get("refresh_token");
        idToken = (String) tokenResponse.get("id_token");
    }

    /**
     * 刷新访问令牌
     */
    protected void refreshAccessToken() throws Exception {
        MvcResult refreshResult = mockMvc.perform(post("/api/oauth2/token")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                    firstPartyClient.getClientId(), webClientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", refreshToken))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> refreshResponse = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                Map.class
        );
        
        // 更新令牌
        accessToken = (String) refreshResponse.get("access_token");
        refreshToken = (String) refreshResponse.get("refresh_token"); // 新的刷新令牌
    }

    /**
     * 设置普通用户的令牌
     */
    protected void setupUserWithToken() throws Exception {
        // 确保用户只有USER角色
        testUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));
        testUser = userRepository.save(testUser);
        userRepository.flush();
        
        // 重新获取token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUser.getUsername());
        loginRequest.setPassword("password123");
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            Map.class
        );
        accessToken = (String) response.get("token");
    }

    /**
     * 获取管理员token
     */
    protected void setupAdminWithToken() throws Exception {
        // 移除普通用户角色，只保留管理员角色
        testUser.getRoles().clear();
        testUser.getRoles().add(adminRole);
        testUser = userRepository.save(testUser);
        
        // 重新完成OAuth2授权流程以获取管理员令牌
        completeOAuth2Flow();
    }

    protected void setupAdminUser() {
        testUser.getRoles().add(adminRole);
        testUser = userRepository.save(testUser);
        userRepository.flush();
    }
} 