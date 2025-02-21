package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.AuthServiceApplication;
import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.request.RegisterRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.dto.response.ErrorResponse;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        userRepository.deleteAll();

        // 准备测试数据
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("test@example.com");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_Success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);

        assertNotNull(response.getToken());
        assertEquals(registerRequest.getUsername(), response.getUsername());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertEquals(1, response.getRoles().size());

        assertTrue(userRepository.existsByUsername(registerRequest.getUsername()));
    }

    @Test
    void register_DuplicateUsername() throws Exception {
        // 先注册一次
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 再次注册同样的用户名
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8), ErrorResponse.class);

        assertEquals("用户名已存在", response.getMessage());
    }

    @Test
    void login_Success() throws Exception {
        // 先注册用户
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 然后尝试登录
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);

        assertNotNull(response.getToken());
        assertEquals(loginRequest.getUsername(), response.getUsername());
        assertTrue(response.getRoles().contains("ROLE_USER"));
    }

    @Test
    void login_WrongPassword() throws Exception {
        // 先注册用户
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(registerRequest)));

        // 使用错误密码登录
        loginRequest.setPassword("wrongpassword");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8), ErrorResponse.class);

        assertEquals("用户名或密码错误", response.getMessage());
    }

    @Test
    void login_AccountLocked() throws Exception {
        // 1. 先注册用户
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 2. 使用错误密码尝试登录5次
        loginRequest.setPassword("wrongpassword");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 3. 第6次登录应该返回账号锁定错误
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                ErrorResponse.class);

        assertEquals("账号已被锁定", response.getMessage());

        // 4. 使用正确密码登录也应该返回锁定错误
        loginRequest.setPassword("password123");
        result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        response = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                ErrorResponse.class);

        assertEquals("账号已被锁定", response.getMessage());
    }

    @Test
    void login_LockExpired() throws Exception {
        // 1. 先注册用户
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 2. 获取用户并手动设置锁定状态（模拟30分钟前锁定）
        User user = userRepository.findByUsername(registerRequest.getUsername()).get();
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now().minusMinutes(31));
        userRepository.save(user);

        // 3. 使用正确密码登录应该成功（因为锁定已过期）
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class);

        assertNotNull(response.getToken());
        assertEquals(loginRequest.getUsername(), response.getUsername());
    }
} 