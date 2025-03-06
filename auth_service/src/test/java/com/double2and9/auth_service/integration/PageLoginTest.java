package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.config.CompleteTestConfig;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.utils.MockPageLoginHelper;
import com.double2and9.auth_service.utils.PKCEUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.Cookie;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 页面登录流程测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CompleteTestConfig.class)
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true"
})
@Transactional
@Disabled("需要完整的集成环境，暂时禁用")
public class PageLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Role userRole;
    private String codeVerifier;
    private String codeChallenge;

    @BeforeEach
    public void setup() {
        // 创建测试角色
        userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    return roleRepository.save(role);
                });

        // 创建测试用户
        Set<Role> roles = new HashSet<>(Collections.singletonList(userRole));
        testUser = userRepository.findByUsername("testuser")
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername("testuser");
                    user.setEmail("test@example.com");
                    user.setPassword(passwordEncoder.encode("password123"));
                    user.setRoles(roles);
                    user.setEmailVerified(true);
                    user.setEnabled(true);
                    return userRepository.save(user);
                });

        // 生成PKCE参数
        this.codeVerifier = PKCEUtils.generateCodeVerifier();
        this.codeChallenge = PKCEUtils.generateCodeChallenge(codeVerifier);
    }

    @Test
    void testBasicLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        
        // 执行表单登录
        MvcResult loginResult = MockPageLoginHelper.performFormLoginAndExpectRedirect(
                mockMvc, "testuser", "password123", session);
        
        // 验证登录成功后的重定向
        String redirectUrl = loginResult.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl, "登录后应重定向");
        
        // 检查是否设置了认证Cookie
        Cookie authCookie = loginResult.getResponse().getCookie("jwt_token");
        assertNotNull(authCookie, "登录后应设置认证Cookie");
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        MockHttpSession session = new MockHttpSession();
        
        // 使用错误的密码尝试登录
        MvcResult loginResult = MockPageLoginHelper.performFormLogin(
                mockMvc, "testuser", "wrongpassword", session);
        
        // 验证登录失败后的重定向
        String redirectUrl = loginResult.getResponse().getRedirectedUrl();
        assertTrue(redirectUrl.contains("/auth/login?error="), "登录失败应重定向到登录页面并显示错误信息");
    }

    @Test
    void testOAuth2Login() throws Exception {
        MockHttpSession session = new MockHttpSession();
        
        // 执行带OAuth2参数的表单登录
        MvcResult loginResult = MockPageLoginHelper.performOAuth2FormLogin(
                mockMvc, 
                "testuser", 
                "password123", 
                session,
                "web-client",
                "http://localhost:3000/callback",
                "state123",
                codeChallenge,
                "S256");
        
        // 验证登录后可能重定向到授权页面
        String redirectUrl = loginResult.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl, "登录后应重定向");
        
        if (redirectUrl.contains("/oauth2/authorize")) {
            // 如果有授权确认页面，模拟用户确认授权
            MvcResult authorizeResult = mockMvc.perform(get(redirectUrl)
                    .session(session))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();
            
            // 验证重定向到客户端回调URL，并包含授权码
            String callbackUrl = authorizeResult.getResponse().getRedirectedUrl();
            assertTrue(callbackUrl.startsWith("http://localhost:3000/callback"), 
                    "应重定向到客户端回调URL");
            assertTrue(callbackUrl.contains("code="), "回调URL应包含授权码");
        }
    }

    @Test
    void testRememberMeLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        
        // 执行带记住我选项的表单登录
        MvcResult loginResult = MockPageLoginHelper.performFormLogin(
                mockMvc, "testuser", "password123", session, true);
        
        // 验证登录成功
        String redirectUrl = loginResult.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl, "登录后应重定向");
        
        // 检查Cookie是否设置了较长的过期时间
        Cookie authCookie = loginResult.getResponse().getCookie("jwt_token");
        assertNotNull(authCookie, "登录后应设置认证Cookie");
        assertTrue(authCookie.getMaxAge() > 7200, "记住我选项应设置较长的Cookie过期时间");
    }
} 