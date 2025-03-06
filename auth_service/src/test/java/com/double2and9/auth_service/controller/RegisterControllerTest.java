package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.RegisterRequest;
import com.double2and9.auth_service.dto.response.CaptchaDTO;
import com.double2and9.auth_service.service.AuthService;
import com.double2and9.auth_service.service.CaptchaService;
import com.double2and9.base.enums.AuthErrorCode;
import com.double2and9.auth_service.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class RegisterControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private CaptchaService captchaService;

    @InjectMocks
    private RegisterController registerController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(registerController).build();
    }

    @Test
    public void testShowRegisterPage() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    public void testShowRegisterPage_WithErrorAndSuccess() throws Exception {
        mockMvc.perform(get("/auth/register")
                .param("error", "测试错误")
                .param("success", "测试成功"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attribute("error", "测试错误"))
                .andExpect(model().attribute("success", "测试成功"));
    }

    @Test
    public void testGetCaptcha() throws Exception {
        // 准备测试数据
        CaptchaDTO captchaDTO = new CaptchaDTO("test-id", "data:image/png;base64,test-image");
        when(captchaService.generateCaptcha()).thenReturn(captchaDTO);

        // 执行测试
        mockMvc.perform(get("/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaId").value("test-id"))
                .andExpect(jsonPath("$.imageBase64").value("data:image/png;base64,test-image"));
    }

    @Test
    public void testProcessRegistration_Success() throws Exception {
        // 准备测试数据
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setConfirmPassword("password");
        request.setEmail("test@example.com");
        request.setCaptchaId("test-id");
        request.setCaptchaCode("ABC123");

        // 模拟验证码验证成功，使用lenient()避免UnnecessaryStubbingException
        lenient().when(captchaService.validateCaptcha("test-id", "ABC123")).thenReturn(true);

        // 执行测试
        mockMvc.perform(post("/auth/register")
                .flashAttr("registerRequest", request))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("success"));

        // 验证服务调用
        verify(authService).register(request);
    }

    @Test
    public void testProcessRegistration_ValidationError() throws Exception {
        // 准备测试数据 - 缺少必要字段
        RegisterRequest request = new RegisterRequest();
        // 不设置任何字段，触发验证错误

        // 执行测试
        mockMvc.perform(post("/auth/register")
                .flashAttr("registerRequest", request))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.registerRequest"))
                .andExpect(flash().attributeExists("registerRequest"));

        // 验证服务未调用
        verify(authService, never()).register(any());
    }

    @Test
    public void testProcessRegistration_ServiceException() throws Exception {
        // 准备测试数据
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setConfirmPassword("password");
        request.setEmail("test@example.com");
        request.setCaptchaId("test-id");
        request.setCaptchaCode("ABC123");

        // 模拟验证码验证成功，使用lenient()避免UnnecessaryStubbingException
        lenient().when(captchaService.validateCaptcha("test-id", "ABC123")).thenReturn(true);

        // 模拟服务抛出异常
        doThrow(new AuthException(AuthErrorCode.USERNAME_ALREADY_EXISTS, HttpStatus.BAD_REQUEST))
                .when(authService).register(request);

        // 执行测试
        mockMvc.perform(post("/auth/register")
                .flashAttr("registerRequest", request))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attributeExists("registerRequest"));
    }
} 