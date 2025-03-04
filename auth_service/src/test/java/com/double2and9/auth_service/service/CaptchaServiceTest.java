package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.CaptchaDTO;
import com.double2and9.auth_service.service.impl.SimpleCaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CaptchaServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SimpleCaptchaService captchaService;

    @BeforeEach
    public void setup() {
        // 使用lenient()避免UnnecessaryStubbingException
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void testGenerateCaptcha() {
        // 执行测试
        CaptchaDTO result = captchaService.generateCaptcha();

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getCaptchaId());
        assertNotNull(result.getImageBase64());
        assertTrue(result.getImageBase64().startsWith("data:image/png;base64,"));

        // 验证Redis存储
        verify(valueOperations).set(
                argThat(key -> key.startsWith("captcha:")),
                anyString(),
                any(Duration.class)
        );
    }

    @Test
    public void testValidateCaptcha_Success() {
        // 准备测试数据
        String captchaId = "test-id";
        String captchaCode = "ABC123";
        String redisKey = "captcha:" + captchaId;

        // 模拟Redis返回
        when(valueOperations.get(redisKey)).thenReturn(captchaCode);

        // 执行测试
        boolean result = captchaService.validateCaptcha(captchaId, captchaCode);

        // 验证结果
        assertTrue(result);
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    public void testValidateCaptcha_CaseInsensitive() {
        // 准备测试数据
        String captchaId = "test-id";
        String captchaCode = "ABC123";
        String userInput = "abc123";
        String redisKey = "captcha:" + captchaId;

        // 模拟Redis返回
        when(valueOperations.get(redisKey)).thenReturn(captchaCode);

        // 执行测试
        boolean result = captchaService.validateCaptcha(captchaId, userInput);

        // 验证结果
        assertTrue(result);
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    public void testValidateCaptcha_InvalidCode() {
        // 准备测试数据
        String captchaId = "test-id";
        String captchaCode = "ABC123";
        String userInput = "XYZ789";
        String redisKey = "captcha:" + captchaId;

        // 模拟Redis返回
        when(valueOperations.get(redisKey)).thenReturn(captchaCode);

        // 执行测试
        boolean result = captchaService.validateCaptcha(captchaId, userInput);

        // 验证结果
        assertFalse(result);
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    public void testValidateCaptcha_ExpiredOrNotFound() {
        // 准备测试数据
        String captchaId = "test-id";
        String userInput = "ABC123";
        String redisKey = "captcha:" + captchaId;

        // 模拟Redis返回null（验证码不存在或已过期）
        when(valueOperations.get(redisKey)).thenReturn(null);

        // 执行测试
        boolean result = captchaService.validateCaptcha(captchaId, userInput);

        // 验证结果
        assertFalse(result);
        verify(redisTemplate, never()).delete(redisKey);
    }

    @Test
    public void testValidateCaptcha_NullInput() {
        // 执行测试
        boolean result = captchaService.validateCaptcha(null, "ABC123");

        // 验证结果
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());

        // 执行测试
        result = captchaService.validateCaptcha("test-id", null);

        // 验证结果
        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }
} 