package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.AuthServiceApplication;
import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.request.RegisterRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@Transactional
class OidcControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 创建测试用户
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    role.setDescription("普通用户");
                    return roleRepository.save(role);
                });

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setGivenName("Test");
        testUser.setFamilyName("User");
        testUser.setRoles(Set.of(userRole));
        testUser = userRepository.save(testUser);

        // 获取访问令牌
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        accessToken = authResponse.getToken();
    }

    @Test
    void getUserInfo_Success() throws Exception {
        mockMvc.perform(get("/oauth2/userinfo")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.given_name").value(testUser.getGivenName()))
                .andExpect(jsonPath("$.family_name").value(testUser.getFamilyName()));
    }

    @Test
    void getConfiguration_Success() throws Exception {
        mockMvc.perform(get("/oauth2/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8084"))
                .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8084/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8084/oauth2/token"))
                .andExpect(jsonPath("$.userinfo_endpoint").value("http://localhost:8084/oauth2/userinfo"))
                .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8084/oauth2/jwks"))
                .andExpect(jsonPath("$.end_session_endpoint").value("http://localhost:8084/oauth2/logout"))
                .andExpect(jsonPath("$.check_session_iframe").value("http://localhost:8084/oauth2/check-session"))
                .andExpect(jsonPath("$.scopes_supported").isArray())
                .andExpect(jsonPath("$.response_types_supported").isArray())
                .andExpect(jsonPath("$.subject_types_supported").isArray())
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported").isArray())
                .andExpect(jsonPath("$.claims_supported").isArray());
    }

    @Test
    void checkSession_Success() throws Exception {
        String clientId = "test-client";
        String sessionState = UUID.randomUUID().toString();

        mockMvc.perform(get("/oauth2/check-session")
                .param("client_id", clientId)
                .param("session_state", sessionState)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void endSession_Success() throws Exception {
        // 生成一个有效的ID Token
        String idToken = jwtService.generateIdToken(
            testUser.getId().toString(),
            "test-client",
            "test-nonce"
        );

        mockMvc.perform(get("/oauth2/end-session")
                .param("id_token_hint", idToken)
                .param("post_logout_redirect_uri", "http://localhost:3000/logout")
                .param("state", "xyz")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void rpInitiatedLogout_Success() throws Exception {
        // 生成一个有效的ID Token
        String idToken = jwtService.generateIdToken(
            testUser.getId().toString(),
            "test-client",
            "test-nonce"
        );

        mockMvc.perform(get("/oauth2/session/end")
                .param("id_token_hint", idToken)
                .param("post_logout_redirect_uri", "http://localhost:3000/logout")
                .param("state", "xyz")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:3000/logout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("xyz")));
    }

    @Test
    void rpInitiatedLogout_WithoutIdTokenHint_Success() throws Exception {
        mockMvc.perform(get("/oauth2/session/end")
                .param("post_logout_redirect_uri", "http://localhost:3000/logout")
                .param("state", "xyz")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:3000/logout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("xyz")));
    }
} 