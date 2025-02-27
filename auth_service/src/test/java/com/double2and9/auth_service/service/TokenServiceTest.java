package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.base.enums.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient.Builder;
import io.jsonwebtoken.Claims;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenServiceTest {

    @Mock
    private CustomJdbcRegisteredClientRepository clientRepository;

    @Mock
    private AuthorizationCodeService authorizationCodeService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TokenService tokenService;

    private TokenRequest request;
    private RegisteredClient client;
    private AuthorizationCode authCode;

    @BeforeEach
    void setUp() {
        // 设置请求数据
        request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setCode("test_code");
        request.setRedirectUri("http://localhost:8080/callback");

        // 设置客户端
        client = RegisteredClient.withId("1")
                .clientId("test_client")
                .clientSecret("test_secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .redirectUri("http://localhost:8080/callback")
                .scope("read")
                .build();

        // 设置授权码
        authCode = new AuthorizationCode();
        authCode.setUserId("test_user");
        authCode.setScope("read write");
    }

    @Test
    void createToken_AuthorizationCode_Success() {
        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(authorizationCodeService.validateAndConsume(
            eq("test_code"), 
            eq("test_client"), 
            eq("http://localhost:8080/callback")
        )).thenReturn(authCode);

        // 修改为使用新的 JwtService 方法
        when(jwtService.generateAccessToken("test_user", "test_client", "read write"))
            .thenReturn("access_token");
        when(jwtService.generateRefreshToken("test_user", "test_client", "read write"))
            .thenReturn("refresh_token");

        TokenResponse response = tokenService.createToken("test_client", "test_secret", request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("read write", response.getScope());

        verify(jwtService).generateAccessToken("test_user", "test_client", "read write");
        verify(jwtService).generateRefreshToken("test_user", "test_client", "read write");
    }

    @Test
    void refreshToken_Success() {
        // 修改请求为刷新令牌模式
        request.setGrantType("refresh_token");
        request.setRefreshToken("valid_refresh_token");
        
        // Mock JWT解析
        Claims claims = Mockito.mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("test_user");
        when(claims.get("clientId", String.class)).thenReturn("test_client");
        when(claims.get("scope", String.class)).thenReturn("read write");
        when(claims.get("type", String.class)).thenReturn("refresh_token");
        
        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(jwtService.validateRefreshToken("valid_refresh_token")).thenReturn(claims);
        when(jwtService.generateAccessToken(eq("test_user"), eq("test_client"), eq("read write")))
            .thenReturn("new_access_token");
        when(jwtService.generateRefreshToken(eq("test_user"), eq("test_client"), eq("read write")))
            .thenReturn("new_refresh_token");

        TokenResponse response = tokenService.createToken("test_client", "test_secret", request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("read write", response.getScope());

        verify(jwtService).validateRefreshToken("valid_refresh_token");
        verify(jwtService).generateAccessToken("test_user", "test_client", "read write");
        verify(jwtService).generateRefreshToken("test_user", "test_client", "read write");
    }

    @Test
    void createToken_WithOpenIdScope_Success() {
        // 准备测试数据
        authCode.setScope("openid profile");
        request.setNonce("test_nonce");

        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(authorizationCodeService.validateAndConsume(
            eq("test_code"), 
            eq("test_client"), 
            eq("http://localhost:8080/callback")
        )).thenReturn(authCode);

        // 修改为使用新的 JwtService 方法
        when(jwtService.generateAccessToken("test_user", "test_client", "openid profile"))
            .thenReturn("access_token");
        when(jwtService.generateRefreshToken("test_user", "test_client", "openid profile"))
            .thenReturn("refresh_token");
        when(jwtService.generateIdToken("test_user", "test_client", "test_nonce"))
            .thenReturn("id_token");

        TokenResponse response = tokenService.createToken("test_client", "test_secret", request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("id_token", response.getIdToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("openid profile", response.getScope());

        verify(jwtService).generateAccessToken("test_user", "test_client", "openid profile");
        verify(jwtService).generateRefreshToken("test_user", "test_client", "openid profile");
        verify(jwtService).generateIdToken("test_user", "test_client", "test_nonce");
    }

    @Test
    void createToken_WithoutOpenIdScope_NoIdToken() {
        // 准备测试数据
        authCode.setScope("profile");

        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(authorizationCodeService.validateAndConsume(
            eq("test_code"), 
            eq("test_client"), 
            eq("http://localhost:8080/callback")
        )).thenReturn(authCode);

        // 修改为使用新的 JwtService 方法
        when(jwtService.generateAccessToken("test_user", "test_client", "profile"))
            .thenReturn("access_token");
        when(jwtService.generateRefreshToken("test_user", "test_client", "profile"))
            .thenReturn("refresh_token");

        TokenResponse response = tokenService.createToken("test_client", "test_secret", request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertNull(response.getIdToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("profile", response.getScope());

        verify(jwtService).generateAccessToken("test_user", "test_client", "profile");
        verify(jwtService).generateRefreshToken("test_user", "test_client", "profile");
        verify(jwtService, never()).generateIdToken(anyString(), anyString(), anyString());
    }

    @Test
    void createToken_InvalidGrantType() {
        request.setGrantType("invalid_grant_type");

        assertThrows(AuthException.class, () -> tokenService.createToken("test_client", "test_secret", request));
    }

    @Test
    void createToken_InvalidClientCredentials() {
        // 使用错误的客户端密钥
        when(clientRepository.findByClientId("test_client")).thenReturn(client);

        assertThrows(AuthException.class, () -> tokenService.createToken("test_client", "wrong_secret", request));
    }

    @Test
    void createTokenByAuthorizationCode_WithValidPKCE_Success() {
        // 准备测试数据
        request.setCodeVerifier("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");
        authCode.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        authCode.setCodeChallengeMethod("S256");

        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(authorizationCodeService.validateAndConsume(
            eq("test_code"), 
            eq("test_client"), 
            eq("http://localhost:8080/callback")
        )).thenReturn(authCode);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString()))
            .thenReturn("access_token");
        when(jwtService.generateRefreshToken(anyString(), anyString(), anyString()))
            .thenReturn("refresh_token");

        TokenResponse response = tokenService.createToken("test_client", "test_secret", request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
    }

    @Test
    void createTokenByAuthorizationCode_WithoutRequiredCodeVerifier_ThrowsException() {
        // 准备测试数据
        authCode.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        authCode.setCodeChallengeMethod("S256");

        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(authorizationCodeService.validateAndConsume(
            eq("test_code"), 
            eq("test_client"), 
            eq("http://localhost:8080/callback")
        )).thenReturn(authCode);

        AuthException exception = assertThrows(AuthException.class, () -> 
            tokenService.createToken("test_client", "test_secret", request));
        
        assertEquals(AuthErrorCode.CODE_VERIFIER_REQUIRED, exception.getErrorCode());
    }

    @Test
    void createTokenByAuthorizationCode_WithInvalidCodeVerifier_ThrowsException() {
        // 准备测试数据
        request.setCodeVerifier("invalid-verifier");
        authCode.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        authCode.setCodeChallengeMethod("S256");

        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(authorizationCodeService.validateAndConsume(
            eq("test_code"), 
            eq("test_client"), 
            eq("http://localhost:8080/callback")
        )).thenReturn(authCode);

        AuthException exception = assertThrows(AuthException.class, () -> 
            tokenService.createToken("test_client", "test_secret", request));
        
        assertEquals(AuthErrorCode.INVALID_CODE_VERIFIER, exception.getErrorCode());
    }

    @Test
    void refreshToken_WithOpenIdScope_Success() {
        // 修改请求为刷新令牌模式
        request.setGrantType("refresh_token");
        request.setRefreshToken("valid_refresh_token");
        request.setNonce("refresh_nonce");  // 添加nonce参数
        
        // Mock JWT解析
        Claims claims = Mockito.mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("test_user");
        when(claims.get("clientId", String.class)).thenReturn("test_client");
        when(claims.get("scope", String.class)).thenReturn("openid profile email");  // 包含openid scope
        when(claims.get("type", String.class)).thenReturn("refresh_token");
        
        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(jwtService.validateRefreshToken("valid_refresh_token")).thenReturn(claims);
        when(jwtService.generateAccessToken(eq("test_user"), eq("test_client"), eq("openid profile email")))
            .thenReturn("new_access_token");
        when(jwtService.generateRefreshToken(eq("test_user"), eq("test_client"), eq("openid profile email")))
            .thenReturn("new_refresh_token");
        when(jwtService.generateIdToken(eq("test_user"), eq("test_client"), eq("refresh_nonce")))
            .thenReturn("new_id_token");

        TokenResponse response = tokenService.createToken("test_client", "test_secret", request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
        assertEquals("new_id_token", response.getIdToken());  // 验证ID Token
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("openid profile email", response.getScope());

        verify(jwtService).validateRefreshToken("valid_refresh_token");
        verify(jwtService).generateAccessToken("test_user", "test_client", "openid profile email");
        verify(jwtService).generateRefreshToken("test_user", "test_client", "openid profile email");
        verify(jwtService).generateIdToken("test_user", "test_client", "refresh_nonce");
    }

    @Test
    void refreshToken_WithOpenIdScope_NoNonce_NoIdToken() {
        // 修改请求为刷新令牌模式，但不提供nonce
        request.setGrantType("refresh_token");
        request.setRefreshToken("valid_refresh_token");
        // 不设置nonce
        
        // Mock JWT解析
        Claims claims = Mockito.mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn("test_user");
        when(claims.get("clientId", String.class)).thenReturn("test_client");
        when(claims.get("scope", String.class)).thenReturn("openid profile email");  // 包含openid scope
        when(claims.get("type", String.class)).thenReturn("refresh_token");
        
        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(jwtService.validateRefreshToken("valid_refresh_token")).thenReturn(claims);
        when(jwtService.generateAccessToken(eq("test_user"), eq("test_client"), eq("openid profile email")))
            .thenReturn("new_access_token");
        when(jwtService.generateRefreshToken(eq("test_user"), eq("test_client"), eq("openid profile email")))
            .thenReturn("new_refresh_token");

        TokenResponse response = tokenService.createToken("test_client", "test_secret", request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
        assertNull(response.getIdToken());  // ID Token应该为null
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("openid profile email", response.getScope());

        verify(jwtService).validateRefreshToken("valid_refresh_token");
        verify(jwtService).generateAccessToken("test_user", "test_client", "openid profile email");
        verify(jwtService).generateRefreshToken("test_user", "test_client", "openid profile email");
        verify(jwtService, never()).generateIdToken(anyString(), anyString(), anyString());  // 验证不生成ID Token
    }
} 