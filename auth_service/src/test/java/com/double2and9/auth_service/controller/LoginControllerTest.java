package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.service.AuthService;
import jakarta.servlet.http.Cookie;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginControllerTest {

    @Mock
    private AuthService authService;

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

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");
        
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
        // When
        String viewName = loginController.showLoginPage(null, null, request, model);
        
        // Then
        assertEquals("auth/login", viewName);
        verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
    }

    @Test
    public void testShowLoginPageWithError() {
        // When
        String viewName = loginController.showLoginPage("Invalid credentials", null, request, model);
        
        // Then
        assertEquals("auth/login", viewName);
        verify(model).addAttribute("error", "Invalid credentials");
        verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
    }

    @Test
    public void testShowLoginPageWithSuccess() {
        // When
        String viewName = loginController.showLoginPage(null, "Registration successful", request, model);
        
        // Then
        assertEquals("auth/login", viewName);
        verify(model).addAttribute("success", "Registration successful");
        verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
    }

    @Test
    public void testShowLoginPageWithOAuth2Parameters() {
        // Given
        SavedRequest savedRequest = mock(SavedRequest.class);
        when(savedRequest.getRedirectUrl()).thenReturn("http://localhost:8080/oauth2/authorize");
        when(requestCache.getRequest(request, null)).thenReturn(savedRequest);
        
        // When
        String viewName = loginController.showLoginPage(null, null, request, model);
        
        // Then
        assertEquals("auth/login", viewName);
        verify(model).addAttribute("continueAuthorization", true);
    }

    @Test
    public void testProcessLoginWithFormErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);
        
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                null, // client_id
                null, // redirect_uri
                null, // response_type
                null, // scope
                null, // state
                null, // code_challenge
                null, // code_challenge_method
                null, // nonce
                null, // continue_authorization
                request,
                response,
                redirectAttributes,
                model
        );
        
        assertEquals("auth/login", viewName);
        verifyNoInteractions(authService);
    }

    @Test
    public void testProcessLoginSuccess() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(requestCache.getRequest(any(), any())).thenReturn(null);
        
        when(authService.login(anyString(), anyString(), anyString())).thenReturn("jwt-token");
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                null, // client_id
                null, // redirect_uri
                null, // response_type
                null, // scope
                null, // state
                null, // code_challenge
                null, // code_challenge_method
                null, // nonce
                null, // continue_authorization
                request,
                response,
                redirectAttributes,
                model
        );
        
        // Then
        assertEquals("redirect:/", viewName);
        
        // 验证Cookie设置
        Cookie cookie = response.getCookie("jwt_token");
        assertEquals("jwt-token", cookie.getValue());
        assertEquals(-1, cookie.getMaxAge()); // 会话Cookie
        assertEquals(true, cookie.isHttpOnly());
    }

    @Test
    public void testProcessLoginWithSavedRequest() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        
        // 模拟有保存的请求
        when(requestCache.getRequest(request, response)).thenReturn(savedRequest);
        when(savedRequest.getRedirectUrl()).thenReturn("http://localhost:8080/profile");
        
        when(authService.login(anyString(), anyString(), anyString())).thenReturn("jwt-token");
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                null, // client_id
                null, // redirect_uri
                null, // response_type
                null, // scope
                null, // state
                null, // code_challenge
                null, // code_challenge_method
                null, // nonce
                null, // continue_authorization
                request,
                response,
                redirectAttributes,
                model
        );
        
        // Then
        assertEquals("redirect:http://localhost:8080/profile", viewName);
        verify(requestCache).removeRequest(request, response);
    }

    @Test
    public void testProcessLoginWithOAuth2Continuation() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authService.login(anyString(), anyString(), anyString())).thenReturn("jwt-token");
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                "test-client", // client_id
                "http://localhost:8080/callback", // redirect_uri
                "code", // response_type
                "openid profile", // scope
                "test-state", // state
                "test-challenge", // code_challenge
                "S256", // code_challenge_method
                "test-nonce", // nonce
                "true", // continue_authorization
                request,
                response,
                redirectAttributes,
                model
        );
        
        // Then
        assertEquals("redirect:/oauth2/authorize?client_id=test-client&redirect_uri=http://localhost:8080/callback&response_type=code&scope=openid profile&state=test-state&code_challenge=test-challenge&code_challenge_method=S256&nonce=test-nonce", viewName);
    }

    @Test
    public void testProcessLoginFailure() throws Exception {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authService.login(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Authentication failed"));
        
        // When
        String viewName = loginController.processLogin(
                loginRequest,
                bindingResult,
                null, // client_id
                null, // redirect_uri
                null, // response_type
                null, // scope
                null, // state
                null, // code_challenge
                null, // code_challenge_method
                null, // nonce
                null, // continue_authorization
                request,
                response,
                redirectAttributes,
                model
        );
        
        // Then
        assertEquals("redirect:/auth/login", viewName);
        verify(redirectAttributes).addFlashAttribute("error", "用户名或密码错误");
    }
} 