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
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient.Builder;
import io.jsonwebtoken.Claims;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
        request.setClientId("test_client");
        request.setClientSecret("test_secret");

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
        when(jwtService.generateAccessToken("test_user", "test_client", "read write"))
            .thenReturn("access_token");
        when(jwtService.generateRefreshToken("test_user", "test_client", "read write"))
            .thenReturn("refresh_token");

        TokenResponse response = tokenService.createToken(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("read write", response.getScope());
    }

    @Test
    void refreshToken_Success() {
        // 修改请求为刷新令牌模式
        request.setGrantType("refresh_token");
        request.setRefreshToken("valid_refresh_token");
        
        // Mock JWT解析
        Claims claims = Mockito.mock(Claims.class);
        when(claims.get(eq("userId"), eq(String.class))).thenReturn("test_user");
        when(claims.get(eq("clientId"), eq(String.class))).thenReturn("test_client");
        when(claims.get(eq("scope"), eq(String.class))).thenReturn("read write");
        when(claims.get(eq("type"), eq(String.class))).thenReturn("refresh_token");
        
        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(jwtService.parseToken("valid_refresh_token")).thenReturn(claims);
        when(jwtService.generateAccessToken("test_user", "test_client", "read write"))
            .thenReturn("new_access_token");
        when(jwtService.generateRefreshToken("test_user", "test_client", "read write"))
            .thenReturn("new_refresh_token");

        TokenResponse response = tokenService.createToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("read write", response.getScope());
    }

    @Test
    void refreshToken_InvalidToken() {
        // 修改请求为刷新令牌模式
        request.setGrantType("refresh_token");
        request.setRefreshToken("invalid_token");
        
        when(clientRepository.findByClientId("test_client")).thenReturn(client);
        when(jwtService.parseToken("invalid_token")).thenThrow(new RuntimeException());

        assertThrows(AuthException.class, () -> tokenService.createToken(request));
    }

    @Test
    void createToken_InvalidGrantType() {
        request.setGrantType("invalid_grant_type");

        assertThrows(AuthException.class, () -> tokenService.createToken(request));
    }

    @Test
    void createToken_InvalidClientCredentials() {
        // 修改请求使用错误的客户端密钥
        request.setClientSecret("wrong_secret");
        
        when(clientRepository.findByClientId("test_client")).thenReturn(client);

        assertThrows(AuthException.class, () -> tokenService.createToken(request));
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

        // 执行测试
        TokenResponse response = tokenService.createToken(request);

        // 验证结果
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

        // 执行测试并验证异常
        AuthException exception = assertThrows(AuthException.class, () -> 
            tokenService.createToken(request));
        
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

        // 执行测试并验证异常
        AuthException exception = assertThrows(AuthException.class, () -> 
            tokenService.createToken(request));
        
        assertEquals(AuthErrorCode.INVALID_CODE_VERIFIER, exception.getErrorCode());
    }
} 