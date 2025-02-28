package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.UserInfoResponse;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcSessionServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private OidcSessionService sessionService;
    
    @Test
    void endSession_WithValidIdToken_Success() {
        // 准备测试数据
        String idTokenHint = "valid.id.token";
        String postLogoutRedirectUri = "http://client.example.com/logout";
        String state = "xyz";
        
        Claims claims = Mockito.mock(Claims.class);
        when(claims.get("aud", String.class)).thenReturn("client-id");
        when(jwtService.parseToken(idTokenHint)).thenReturn(claims);
        
        // 执行测试
        assertDoesNotThrow(() -> 
            sessionService.endSession(idTokenHint, postLogoutRedirectUri, state));
        
        // 验证会话清理
        verify(redisTemplate).delete(anyString());
    }
    
    @Test
    void checkSession_ValidSession_ReturnsTrue() {
        String clientId = "test-client";
        String sessionState = "session-state";
        
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        boolean result = sessionService.checkSession(clientId, sessionState);
        
        assertTrue(result);
        verify(redisTemplate).hasKey(contains(clientId));
        verify(redisTemplate).hasKey(contains(sessionState));
    }
    
    @Test
    void handleRpInitiatedLogout_Success() {
        // 准备测试数据
        String idTokenHint = "valid.id.token";
        String postLogoutRedirectUri = "http://client.example.com/logout";
        String state = "xyz";
        
        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtService.parseToken(idTokenHint)).thenReturn(claims);
        
        UserInfoResponse userInfo = UserInfoResponse.builder()
            .sub("1")
            .name("Test User")
            .build();
            
        when(userService.getUserInfo(1L)).thenReturn(userInfo);
        
        // 执行测试
        String redirectUri = sessionService.handleRpInitiatedLogout(
            idTokenHint, postLogoutRedirectUri, state);
        
        // 验证结果
        assertNotNull(redirectUri);
        assertTrue(redirectUri.contains(postLogoutRedirectUri));
        assertTrue(redirectUri.contains("state=" + state));
        
        // 验证会话清理
        verify(redisTemplate).delete(anyString());
    }
} 