package com.double2and9.auth_service.service;

import com.double2and9.auth_service.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcAuthorizationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OidcAuthorizationService oidcAuthorizationService;

    @Test
    void validateAuthorizationRequest_Success() {
        // Given
        String clientId = "test-client";
        String nonce = "test-nonce";
        String scope = "openid profile";
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        oidcAuthorizationService.validateAuthorizationRequest(clientId, nonce, scope);

        // Then
        verify(valueOperations).set(
            eq("oidc:nonce:test-client:test-nonce"),
            eq(nonce),
            eq(10L),
            eq(TimeUnit.MINUTES)
        );
    }

    @Test
    void validateAuthorizationRequest_WithoutOpenidScope_ThrowsException() {
        // Given
        String clientId = "test-client";
        String nonce = "test-nonce";
        String scope = "profile email";

        // When & Then
        AuthException exception = assertThrows(AuthException.class, () ->
            oidcAuthorizationService.validateAuthorizationRequest(clientId, nonce, scope)
        );
        assertEquals("无效的授权范围", exception.getMessage());
    }

    @Test
    void validateAuthorizationRequest_WithoutNonce_Success() {
        // Given
        String clientId = "test-client";
        String scope = "openid profile";

        // When
        oidcAuthorizationService.validateAuthorizationRequest(clientId, null, scope);

        // Then
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void validateAndConsumeNonce_Success() {
        // Given
        String clientId = "test-client";
        String nonce = "test-nonce";
        String key = "oidc:nonce:test-client:test-nonce";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(nonce);

        // When
        boolean result = oidcAuthorizationService.validateAndConsumeNonce(clientId, nonce);

        // Then
        assertTrue(result);
        verify(redisTemplate).<String>delete(key);
    }

    @Test
    void validateAndConsumeNonce_NonExistentNonce_ReturnsFalse() {
        // Given
        String clientId = "test-client";
        String nonce = "test-nonce";
        String key = "oidc:nonce:test-client:test-nonce";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        // When
        boolean result = oidcAuthorizationService.validateAndConsumeNonce(clientId, nonce);

        // Then
        assertFalse(result);
        verify(redisTemplate, never()).<String>delete((String) any());
    }

    @Test
    void validateAndConsumeNonce_EmptyNonce_Success() {
        // When
        boolean result = oidcAuthorizationService.validateAndConsumeNonce("test-client", "");

        // Then
        assertTrue(result);  // 空的nonce应该返回true，因为nonce是可选的
        verify(valueOperations, never()).get(any());
        verify(redisTemplate, never()).delete((String) any());
    }
} 