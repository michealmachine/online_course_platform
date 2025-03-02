package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.AuthorizationRequest;
import com.double2and9.auth_service.dto.request.ConsentRequest;
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
    private ClientService clientService;

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
        request.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        request.setCodeChallengeMethod("S256");

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
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);
        when(clientService.isInternalClient("test-client")).thenReturn(false);
        when(clientService.isAutoApproveClient("test-client")).thenReturn(false);

        var response = authorizationService.createAuthorizationRequest(request, authentication);

        assertNotNull(response);
        assertEquals(request.getClientId(), response.getClientId());
        assertEquals(client.getClientName(), response.getClientName());
        assertEquals(Set.of("read", "write"), response.getRequestedScopes());
        assertEquals(request.getState(), response.getState());
        assertNotNull(response.getAuthorizationId());
        assertEquals(request.getCodeChallenge(), response.getCodeChallenge());
        assertEquals(request.getCodeChallengeMethod(), response.getCodeChallengeMethod());
        
        // 验证保存授权请求
        verify(authorizationConsentService).savePendingAuthorization(any(), any());
        // 验证没有自动授权
        verify(authorizationConsentService, never()).consent(any(ConsentRequest.class));
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
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);
        when(clientService.isInternalClient("test-client")).thenReturn(false);
        when(clientService.isAutoApproveClient("test-client")).thenReturn(false);

        // 执行测试
        AuthorizationResponse response = authorizationService.createAuthorizationRequest(request, authentication);

        // 验证结果
        assertNotNull(response);
        assertEquals(request.getCodeChallenge(), response.getCodeChallenge());
        assertEquals(request.getCodeChallengeMethod(), response.getCodeChallengeMethod());
    }

    @Test
    void createAuthorizationRequest_WithoutPKCE_ThrowsException() {
        // 移除PKCE参数
        request.setCodeChallenge(null);
        request.setCodeChallengeMethod(null);
        
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

    @Test
    void createAuthorizationRequest_WithOpenIdScope_Success() {
        // 准备测试数据
        request.setScope("openid profile email");
        
        RegisteredClient oidcClient = RegisteredClient.withId("1")
                .clientId("test-client")
                .clientName("Test Client")
                .clientIdIssuedAt(Instant.now())
                .redirectUri("http://localhost:8080/callback")
                .scope("openid")
                .scope("profile")
                .scope("email")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(oidcClient);
        when(clientService.isInternalClient("test-client")).thenReturn(false);
        when(clientService.isAutoApproveClient("test-client")).thenReturn(false);

        // 执行测试
        AuthorizationResponse response = authorizationService.createAuthorizationRequest(request, authentication);

        // 验证结果
        assertNotNull(response);
        assertEquals("test-client", response.getClientId());
        assertEquals("Test Client", response.getClientName());
        assertTrue(response.getRequestedScopes().containsAll(Set.of("openid", "profile", "email")));
        assertEquals("xyz", response.getState());
        assertNotNull(response.getAuthorizationId());
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", response.getCodeChallenge());
        assertEquals("S256", response.getCodeChallengeMethod());
    }

    @Test
    void createAuthorizationRequest_WithInvalidOpenIdScope_ThrowsException() {
        // 准备测试数据
        request.setScope("openid profile email");

        // 创建一个不支持openid scope的客户端
        RegisteredClient client = RegisteredClient.withId("1")
                .clientId("test-client")
                .clientName("Test Client")
                .clientIdIssuedAt(Instant.now())
                .redirectUri("http://localhost:8080/callback")
                .scope("profile")
                .scope("email")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId(eq("test-client"))).thenReturn(client);

        // 执行测试并验证异常
        AuthException exception = assertThrows(AuthException.class, () ->
            authorizationService.createAuthorizationRequest(request, authentication));

        assertEquals(AuthErrorCode.CLIENT_SCOPE_INVALID, exception.getErrorCode());
    }
    
    // 新增测试：内部客户端自动授权测试
    @Test
    void createAuthorizationRequest_InternalClient_AutoApprove_Success() {
        // 设置内部客户端和自动授权标识
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);
        when(authentication.getName()).thenReturn("testuser");
        when(clientService.isInternalClient("test-client")).thenReturn(true);
        when(clientService.isAutoApproveClient("test-client")).thenReturn(true);
        
        // 模拟自动授权
        String generatedCode = "auto-generated-code";
        when(authorizationConsentService.consent(any(ConsentRequest.class))).thenReturn(generatedCode);
        
        // 执行测试
        AuthorizationResponse response = authorizationService.createAuthorizationRequest(request, authentication);
        
        // 验证结果
        assertNotNull(response);
        assertEquals(generatedCode, response.getAuthorizationCode());
        
        // 验证调用了自动授权方法而不是保存待处理授权
        verify(authorizationConsentService).consent(any(ConsentRequest.class));
        verify(authorizationConsentService, never()).savePendingAuthorization(any(), any());
        
        // 验证传递给consent方法的参数正确
        verify(authorizationConsentService).consent(argThat(consentRequest -> {
            return consentRequest.getClientId().equals("test-client") &&
                   consentRequest.getUserId().equals("testuser") &&
                   consentRequest.getRedirectUri().equals("http://localhost:8080/callback") &&
                   consentRequest.getScopes().containsAll(Set.of("read", "write"));
        }));
    }
    
    // 新增测试：内部客户端但不自动授权
    @Test
    void createAuthorizationRequest_InternalClient_NotAutoApprove_Success() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);
        when(clientService.isInternalClient("test-client")).thenReturn(true);
        when(clientService.isAutoApproveClient("test-client")).thenReturn(false);
        
        // 执行测试
        AuthorizationResponse response = authorizationService.createAuthorizationRequest(request, authentication);
        
        // 验证结果
        assertNotNull(response);
        assertNull(response.getAuthorizationCode());
        
        // 验证调用了保存待处理授权而不是自动授权
        verify(authorizationConsentService, never()).consent(any(ConsentRequest.class));
        verify(authorizationConsentService).savePendingAuthorization(any(), any());
    }
    
    // 新增测试：非内部客户端但标记为自动授权（应不生效）
    @Test
    void createAuthorizationRequest_NotInternalClient_AutoApprove_Success() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clientRepository.findByClientId("test-client")).thenReturn(client);
        when(clientService.isInternalClient("test-client")).thenReturn(false);
        when(clientService.isAutoApproveClient("test-client")).thenReturn(true);
        
        // 执行测试
        AuthorizationResponse response = authorizationService.createAuthorizationRequest(request, authentication);
        
        // 验证结果
        assertNotNull(response);
        assertNull(response.getAuthorizationCode());
        
        // 验证调用了保存待处理授权而不是自动授权
        verify(authorizationConsentService, never()).consent(any(ConsentRequest.class));
        verify(authorizationConsentService).savePendingAuthorization(any(), any());
    }
} 