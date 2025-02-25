package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.AuthorizationConsentRequest;
import com.double2and9.auth_service.dto.response.AuthorizationResponse;
import com.double2and9.auth_service.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.util.Set;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationConsentServiceTest {

    private static final String REDIS_KEY_PREFIX = "oauth2:auth:request:";

    @Mock
    private RegisteredClientRepository clientRepository;

    @Mock
    private OAuth2AuthorizationService oauth2AuthorizationService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Authentication authentication;

    @Mock
    private AuthorizationCodeService authorizationCodeService;

    @InjectMocks
    private AuthorizationConsentService authorizationConsentService;

    private AuthorizationConsentRequest request;
    private AuthorizationResponse authorizationResponse;
    private RegisteredClient client;

    @BeforeEach
    void setUp() {
        // 设置请求数据
        request = new AuthorizationConsentRequest();
        request.setAuthorizationId("test-auth-id");
        Set<String> approvedScopes = Set.of("read", "write");
        request.setApprovedScopes(approvedScopes);

        // 设置授权响应
        authorizationResponse = new AuthorizationResponse();
        authorizationResponse.setClientId("test-client");
        authorizationResponse.setState("xyz");
        authorizationResponse.setRedirectUri("http://localhost:8080/callback");

        // 设置客户端
        client = RegisteredClient.withId("1")
                .clientId("test-client")
                .clientName("Test Client")
                .clientIdIssuedAt(Instant.now())
                .redirectUri("http://localhost:8080/callback")
                .scope("read")
                .scope("write")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .build();
    }

    @AfterEach
    void tearDown() {
        // 清理 Redis 数据
        redisTemplate.delete(REDIS_KEY_PREFIX + "test-auth-id");
    }

    @Test
    void consent_Success() {
        // 设置所有需要的 mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(valueOperations.get(eq(REDIS_KEY_PREFIX + "test-auth-id"))).thenReturn(authorizationResponse);
        when(clientRepository.findByClientId(eq("test-client"))).thenReturn(client);
        when(authentication.getName()).thenReturn("testUser");

        when(authorizationCodeService.createAuthorizationCode(
            eq("test-client"),
            eq("testUser"),
            eq("http://localhost:8080/callback"),
            anyString(),
            isNull(),
            isNull()
        )).thenReturn("test-code");

        var response = authorizationConsentService.consent(request, authentication);

        assertNotNull(response);
        assertEquals("test-code", response.getAuthorizationCode());
        assertEquals("xyz", response.getState());
        assertEquals("http://localhost:8080/callback", response.getRedirectUri());
    }

    @Test
    void consent_Unauthorized() {
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(AuthException.class, () -> 
            authorizationConsentService.consent(request, authentication));
    }

    @Test
    void consent_RequestNotFound() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        request.setAuthorizationId("non-existent");
        assertThrows(AuthException.class, () -> 
            authorizationConsentService.consent(request, authentication));
    }

    @Test
    void consent_InvalidScopes() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(valueOperations.get(eq(REDIS_KEY_PREFIX + "test-auth-id"))).thenReturn(authorizationResponse);
        when(clientRepository.findByClientId(eq("test-client"))).thenReturn(client);

        request.setApprovedScopes(Set.of("read", "write", "delete"));
        assertThrows(AuthException.class, () -> 
            authorizationConsentService.consent(request, authentication));
    }
    
    // 新增测试方法：测试获取授权请求信息
    @Test
    void getAuthorizationRequest_Success() {
        // 设置mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(valueOperations.get(eq(REDIS_KEY_PREFIX + "test-auth-id"))).thenReturn(authorizationResponse);
        when(clientRepository.findByClientId(eq("test-client"))).thenReturn(client);
        
        // 调用方法
        AuthorizationResponse response = authorizationConsentService.getAuthorizationRequest("test-auth-id", authentication);
        
        // 验证结果
        assertNotNull(response);
        assertEquals("test-client", response.getClientId());
        assertEquals("xyz", response.getState());
        assertEquals("http://localhost:8080/callback", response.getRedirectUri());
    }
    
    @Test
    void getAuthorizationRequest_Unauthorized() {
        when(authentication.isAuthenticated()).thenReturn(false);
        
        assertThrows(AuthException.class, () -> 
            authorizationConsentService.getAuthorizationRequest("test-auth-id", authentication));
    }
    
    @Test
    void getAuthorizationRequest_NotFound() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        
        assertThrows(AuthException.class, () -> 
            authorizationConsentService.getAuthorizationRequest("non-existent", authentication));
    }
    
    @Test
    void getAuthorizationRequest_ClientNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(valueOperations.get(eq(REDIS_KEY_PREFIX + "test-auth-id"))).thenReturn(authorizationResponse);
        when(clientRepository.findByClientId(eq("test-client"))).thenReturn(null);
        
        assertThrows(AuthException.class, () -> 
            authorizationConsentService.getAuthorizationRequest("test-auth-id", authentication));
    }
} 