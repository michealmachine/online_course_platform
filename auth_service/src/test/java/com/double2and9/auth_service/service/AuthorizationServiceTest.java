package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.AuthorizationRequest;
import com.double2and9.auth_service.dto.response.AuthorizationResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.base.enums.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private CustomJdbcRegisteredClientRepository clientRepository;

    @Mock
    private AuthorizationConsentService authorizationConsentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthorizationService authorizationService;

    private AuthorizationRequest request;
    private RegisteredClient client;

    @BeforeEach
    void setUp() {
        // 准备授权请求
        request = new AuthorizationRequest();
        request.setResponseType("code");
        request.setClientId("test-client");
        request.setRedirectUri("http://localhost:8080/callback");
        request.setScope("read write");
        request.setState("xyz");

        // 准备客户端
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

    @Test
    void createAuthorizationRequest_Success() {
        // 添加PKCE参数
        request.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        request.setCodeChallengeMethod("S256");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);

        var response = authorizationService.createAuthorizationRequest(request, authentication);

        assertNotNull(response);
        assertEquals(request.getClientId(), response.getClientId());
        assertEquals(client.getClientName(), response.getClientName());
        assertEquals(Set.of("read", "write"), response.getRequestedScopes());
        assertEquals(request.getState(), response.getState());
        assertNotNull(response.getAuthorizationId());
        assertEquals(request.getCodeChallenge(), response.getCodeChallenge());
        assertEquals(request.getCodeChallengeMethod(), response.getCodeChallengeMethod());
    }

    @Test
    void createAuthorizationRequest_Unauthorized() {
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(AuthException.class, () -> 
            authorizationService.createAuthorizationRequest(request, authentication));
    }

    @Test
    void createAuthorizationRequest_InvalidResponseType() {
        request.setResponseType("token");
        when(authentication.isAuthenticated()).thenReturn(true);

        assertThrows(AuthException.class, () -> 
            authorizationService.createAuthorizationRequest(request, authentication));
    }

    @Test
    void createAuthorizationRequest_ClientNotFound() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(null);

        assertThrows(AuthException.class, () -> 
            authorizationService.createAuthorizationRequest(request, authentication));
    }

    @Test
    void createAuthorizationRequest_InvalidRedirectUri() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);
        request.setRedirectUri("http://evil.com");

        assertThrows(AuthException.class, () -> 
            authorizationService.createAuthorizationRequest(request, authentication));
    }

    @Test
    void createAuthorizationRequest_InvalidScope() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);
        request.setScope("read write delete");

        assertThrows(AuthException.class, () -> 
            authorizationService.createAuthorizationRequest(request, authentication));
    }

    @Test
    void createAuthorizationRequest_WithValidPKCE_Success() {
        // 准备测试数据
        request.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        request.setCodeChallengeMethod("S256");
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);

        // 执行测试
        AuthorizationResponse response = authorizationService.createAuthorizationRequest(request, authentication);

        // 验证结果
        assertNotNull(response);
        assertEquals(request.getCodeChallenge(), response.getCodeChallenge());
        assertEquals(request.getCodeChallengeMethod(), response.getCodeChallengeMethod());
    }

    @Test
    void createAuthorizationRequest_WithoutPKCE_ThrowsException() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);

        // 执行测试并验证异常
        AuthException exception = assertThrows(AuthException.class, () -> 
            authorizationService.createAuthorizationRequest(request, authentication));
        
        assertEquals(AuthErrorCode.PKCE_REQUIRED, exception.getErrorCode());
    }

    @Test
    void createAuthorizationRequest_WithInvalidMethod_ThrowsException() {
        // 准备测试数据
        request.setCodeChallenge("test-challenge");
        request.setCodeChallengeMethod("invalid-method");
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);

        // 执行测试并验证异常
        AuthException exception = assertThrows(AuthException.class, () -> 
            authorizationService.createAuthorizationRequest(request, authentication));
        
        assertEquals(AuthErrorCode.INVALID_CODE_CHALLENGE_METHOD, exception.getErrorCode());
    }
} 