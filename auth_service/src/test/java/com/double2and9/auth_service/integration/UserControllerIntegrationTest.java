package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.AuthServiceApplication;
import com.double2and9.auth_service.dto.request.CreateUserRequest;
import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.request.UpdateUserRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.service.AuthService;
import com.double2and9.auth_service.service.UserService;
import com.double2and9.base.enums.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

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
    private AuthService authService;

    @Autowired
    private UserService userService;

    private User testUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // 创建测试用户
        testUser = createTestUser("testuser", "ROLE_USER");
        // 创建管理员用户并获取token
        adminToken = getAdminToken();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_Success() throws Exception {
        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", "10")
                .param("username", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUser_Success() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_Success() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNickname("新昵称");
        request.setPhone("13800138000");

        mockMvc.perform(put("/api/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("新昵称"))
                .andExpect(jsonPath("$.phone").value("13800138000"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_Success() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");
        request.setRoles(Set.of("ROLE_USER"));
        request.setNickname("New User");
        request.setPhone("13800138000");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.nickname").value("New User"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void createUser_NoPermission() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");
        request.setRoles(Set.of("ROLE_USER"));

        // 使用普通用户token
        String userToken = getUserToken();

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.PERMISSION_DENIED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_DuplicateUsername() throws Exception {
        // 先创建一个用户
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setEmail("existing@example.com");
        request.setRoles(Set.of("ROLE_USER"));
        userService.createUser(request);

        // 尝试创建同名用户
        request.setEmail("another@example.com");
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.USERNAME_ALREADY_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.USERNAME_ALREADY_EXISTS.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_InvalidRole() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");
        request.setRoles(Set.of("ROLE_INVALID"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ROLE.getCode()));
    }

    private User createTestUser(String username, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    newRole.setDescription("Test " + roleName);
                    return roleRepository.save(newRole);
                });

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setEmail(username + "@example.com");
        user.setEnabled(true);
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        
        return userRepository.save(user);
    }

    private String getAdminToken() {
        // 创建管理员用户
        User adminUser = createTestUser("admin", "ROLE_ADMIN");
        
        // 登录获取token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password123");
        
        AuthResponse response = authService.login(loginRequest, "127.0.0.1");
        return response.getToken();
    }

    private String getUserToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        AuthResponse response = authService.login(loginRequest, "127.0.0.1");
        return response.getToken();
    }
} 