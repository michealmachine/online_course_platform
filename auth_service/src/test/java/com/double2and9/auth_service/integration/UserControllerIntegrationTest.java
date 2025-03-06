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
class UserControllerIntegrationTest extends BaseOAuth2IntegrationTest {

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

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();  // 先调用父类的 setUp
        testUser = super.testUser;  // 获取父类创建的 testUser
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void getUsers_Success() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .param("page", "0")
                .param("size", "10")
                .param("username", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void getUser_Success() throws Exception {
        setupAdminUser();  // 添加管理员权限
        mockMvc.perform(get("/api/users/{id}", testUser.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void updateUser_Success() throws Exception {
        setupAdminUser();  // 添加管理员权限
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNickname("新昵称");
        request.setPhone("13800138000");

        mockMvc.perform(put("/api/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("新昵称"))
                .andExpect(jsonPath("$.phone").value("13800138000"));
    }



    @Test
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
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.nickname").value("New User"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void createUser_NoPermission() throws Exception {
        // 使用普通用户token
        setupUserWithToken();

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");
        request.setRoles(Set.of("ROLE_USER"));

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.PERMISSION_DENIED.getMessage()));
    }

    @Test
    void createUser_DuplicateUsername() throws Exception {
        // 先创建一个用户
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setEmail("existing@example.com");
        request.setRoles(Set.of("ROLE_USER"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 尝试创建同名用户
        request.setEmail("another@example.com");
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.USERNAME_ALREADY_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.USERNAME_ALREADY_EXISTS.getMessage()));
    }

    @Test
    void createUser_InvalidRole() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");
        request.setRoles(Set.of("ROLE_INVALID"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_ROLE.getCode()));
    }

    @Test
    void createUser_WithOidcInfo_Success() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("oidcuser");
        request.setPassword("password123");
        request.setEmail("oidc@example.com");
        request.setRoles(Set.of("ROLE_USER"));
        request.setNickname("OIDC User");
        request.setPhone("13800138000");
        
        // 添加OIDC相关信息
        request.setGivenName("John");
        request.setFamilyName("Doe");
        request.setMiddleName("M");
        request.setPreferredUsername("johndoe");
        request.setProfile("http://example.com/johndoe");
        request.setWebsite("http://johndoe.com");
        request.setGender("male");
        request.setBirthdate("1990-01-01");
        request.setZoneinfo("Asia/Shanghai");
        request.setLocale("zh-CN");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("oidcuser"))
                .andExpect(jsonPath("$.email").value("oidc@example.com"))
                .andExpect(jsonPath("$.givenName").value("John"))
                .andExpect(jsonPath("$.familyName").value("Doe"))
                .andExpect(jsonPath("$.middleName").value("M"))
                .andExpect(jsonPath("$.preferredUsername").value("johndoe"))
                .andExpect(jsonPath("$.gender").value("male"))
                .andExpect(jsonPath("$.birthdate").value("1990-01-01"))
                .andExpect(jsonPath("$.zoneinfo").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.locale").value("zh-CN"));
    }

    @Test
    void updateUser_WithOidcInfo_Success() throws Exception {
        setupAdminUser();  // 添加管理员权限
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNickname("Updated OIDC User");
        request.setPhone("13900139000");
        request.setGivenName("Jane");
        request.setFamilyName("Smith");
        request.setMiddleName("A");
        request.setPreferredUsername("janesmith");
        request.setGender("female");
        request.setBirthdate("1992-02-02");
        request.setZoneinfo("America/New_York");
        request.setLocale("en-US");

        mockMvc.perform(put("/api/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Updated OIDC User"))
                .andExpect(jsonPath("$.phone").value("13900139000"))
                .andExpect(jsonPath("$.givenName").value("Jane"))
                .andExpect(jsonPath("$.familyName").value("Smith"))
                .andExpect(jsonPath("$.middleName").value("A"))
                .andExpect(jsonPath("$.preferredUsername").value("janesmith"))
                .andExpect(jsonPath("$.gender").value("female"))
                .andExpect(jsonPath("$.birthdate").value("1992-02-02"))
                .andExpect(jsonPath("$.zoneinfo").value("America/New_York"))
                .andExpect(jsonPath("$.locale").value("en-US"));
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

    /**
     * 设置普通用户的令牌
     */
    public void setupUserWithToken() throws Exception {
        setupUserToken();
    }
} 