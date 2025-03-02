package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.request.OAuth2AuthorizationRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.service.AuthService;
import com.double2and9.auth_service.service.AuthorizationRequestService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthorizationRequestService authorizationRequestService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private RequestCache requestCache;

    @Mock
    private SavedRequest savedRequest;

    @InjectMocks
    private LoginController loginController;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private LoginRequest loginRequest;
    private OAuth2AuthorizationRequest authorizationRequest;

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");
        
        authorizationRequest = OAuth2AuthorizationRequest.builder()
                .clientId("test-client")
                .scope("openid profile")
                .state("test-state")
                .redirectUri("http://localhost:8080/callback")
                .responseType("code")
                .codeChallenge("test-challenge")
                .codeChallengeMethod("S256")
                .continueAuthorization(true)
                .build();
        
        // 使用反射设置requestCache字段
        try {
            java.lang.reflect.Field field = LoginController.class.getDeclaredField("requestCache");
            field.setAccessible(true);
            field.set(loginController, requestCache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testShowLoginPage() {
        // Given
        when(authorizationRequestService.extractAuthorizationRequest(any())).thenReturn(null);
        
        // When
        String viewName = loginController.showLoginPage(null, null, request, response, model);
        
        // Then
        assertEquals("auth/login", viewName);
        verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
        verify(authorizationRequestService).extractAuthorizationRequest(request);
        verify(authorizationRequestService, never()).saveAuthorizationRequest(any(), any(), any());
    }

    @Test
    public void testShowLoginPageWithError() {
        // Given
        when(authorizationRequestService.extractAuthorizationRequest(any())).thenReturn(null);
        
        // When
        String viewName = loginController.showLoginPage("Invalid credentials", null, request, response, model);
        
        // Then
        assertEquals("auth/login", viewName);
        verify(model).addAttribute("error", "登录失败：Invalid credentials");
        verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
    }

    @Test
    public void testShowLoginPageWithSuccess() {
        // Given
        when(authorizationRequestService.extractAuthorizationRequest(any())).thenReturn(null);
        
        // When
        String viewName = loginController.showLoginPage(null, "Registration successful", request, response, model);
        
        // Then
        assertEquals("auth/login", viewName);
        verify(model).addAttribute("success", "Registration successful");
        verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
    }

    @Test
    public void testShowLoginPageWithOAuth2Parameters() {
        // Given
        when(authorizationRequestService.extractAuthorizationRequest(request)).thenReturn(authorizationRequest);
        
        // When
        String viewName = loginController.showLoginPage(null, null, request, response, model);
        
        // Then
        assertEquals("auth/login", viewName);
        verify(authorizationRequestService).saveAuthorizationRequest(authorizationRequest, request, response);
        verify(model).addAttribute("continueAuthorization", true);
    }

    @Test
    public void testShowRegisterPage() {
        String viewName = loginController.showRegisterPage();
        assertEquals("auth/login", viewName);
    }

    @Test
    public void testProcessLoginWithFormErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);
        
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                false,
                request,
                response,
                redirectAttributes
        );
        
        assertEquals("redirect:/auth/login", viewName);
        verify(redirectAttributes).addAttribute("error", "请输入有效的用户名和密码");
        verify(authService, never()).login(any(LoginRequest.class), anyString());
    }

    @Test
    public void testProcessLoginSuccess() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authorizationRequestService.getAuthorizationRequest(any(), any())).thenReturn(null);
        
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token")
                .username("testuser")
                .roles(Collections.emptySet())
                .build();
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(authResponse);
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                false,
                request,
                response,
                redirectAttributes
        );
        
        // Then
        assertEquals("redirect:/", viewName);
        
        // 验证Cookie设置
        Cookie cookie = response.getCookie("auth_token");
        assertEquals("jwt-token", cookie.getValue());
        assertEquals(7200, cookie.getMaxAge()); // 2小时
        assertEquals(true, cookie.isHttpOnly());
    }

    @Test
    public void testProcessLoginSuccessWithRememberMe() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authorizationRequestService.getAuthorizationRequest(any(), any())).thenReturn(null);
        
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token")
                .username("testuser")
                .roles(Collections.emptySet())
                .build();
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(authResponse);
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                true, // 开启记住我
                request,
                response,
                redirectAttributes
        );
        
        // Then
        assertEquals("redirect:/", viewName);
        
        // 验证Cookie设置
        Cookie cookie = response.getCookie("auth_token");
        assertEquals("jwt-token", cookie.getValue());
        assertEquals(604800, cookie.getMaxAge()); // 7天
        assertEquals(true, cookie.isHttpOnly());
    }

    @Test
    public void testProcessLoginWithSavedRequest() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authorizationRequestService.getAuthorizationRequest(any(), any())).thenReturn(null);
        
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token")
                .username("testuser")
                .roles(Collections.emptySet())
                .build();
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(authResponse);
        
        // 模拟有保存的请求
        when(requestCache.getRequest(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn(savedRequest);
        when(savedRequest.getRedirectUrl()).thenReturn("http://localhost:8084/some-protected-url");
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                false,
                request,
                response,
                redirectAttributes
        );
        
        // Then
        // 应该返回null因为已经手动调用了sendRedirect
        assertNull(viewName);
        assertEquals("http://localhost:8084/some-protected-url", response.getRedirectedUrl());
        verify(requestCache).removeRequest(request, response);
    }

    @Test
    public void testProcessLoginWithOAuth2Continuation() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token")
                .username("testuser")
                .roles(Collections.emptySet())
                .build();
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn(authResponse);
        
        // 模拟有保存的OAuth2授权请求
        when(authorizationRequestService.getAuthorizationRequest(request, response))
                .thenReturn(authorizationRequest);
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                false,
                request,
                response,
                redirectAttributes
        );
        
        // Then
        assertNull(viewName); // 应该返回null因为已经手动调用了sendRedirect
        assertEquals("/oauth2/authorize?client_id=test-client&scope=openid profile&state=test-state&redirect_uri=http://localhost:8080/callback&response_type=code&code_challenge=test-challenge&code_challenge_method=S256", 
                response.getRedirectedUrl());
        verify(authorizationRequestService).removeAuthorizationRequest(request, response);
    }

    @Test
    public void testProcessLoginFailure() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authService.login(any(LoginRequest.class), anyString())).thenThrow(new RuntimeException("认证失败"));
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                false,
                request,
                response,
                redirectAttributes
        );
        
        // Then
        assertEquals("redirect:/auth/login", viewName);
        verify(redirectAttributes).addAttribute("error", "认证失败");
    }
} 