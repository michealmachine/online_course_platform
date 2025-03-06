package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.utils.PKCEUtils;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Claims;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final CustomJdbcRegisteredClientRepository clientRepository;
    private final AuthorizationCodeService authorizationCodeService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    
    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String TOKEN_TYPE = "Bearer";
    private static final long ACCESS_TOKEN_EXPIRES_IN = 3600L;  // 访问令牌1小时过期

    @Transactional
    public TokenResponse createToken(String clientId, String clientSecret, TokenRequest request) {
        // 验证客户端凭据是否提供
        if (clientId == null || clientId.isEmpty()) {
            throw new AuthException(AuthErrorCode.PARAMETER_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        
        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new AuthException(AuthErrorCode.PARAMETER_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        
        return switch (request.getGrantType()) {
            case GRANT_TYPE_AUTHORIZATION_CODE -> createTokenByAuthorizationCode(clientId, clientSecret, request);
            case GRANT_TYPE_REFRESH_TOKEN -> refreshToken(clientId, clientSecret, request);
            default -> throw new AuthException(AuthErrorCode.INVALID_GRANT_TYPE);
        };
    }
    
    /**
     * 使用授权码获取令牌
     */
    private TokenResponse createTokenByAuthorizationCode(String clientId, String clientSecret, TokenRequest request) {
        // 验证客户端
        RegisteredClient client = validateClient(clientId, clientSecret);

        // 验证并消费授权码
        AuthorizationCode authCode = authorizationCodeService.validateAndConsume(
            request.getCode(),
            clientId,
            request.getRedirectUri()
        );

        // 验证PKCE
        if (authCode.getCodeChallenge() != null) {
            if (request.getCodeVerifier() == null) {
                throw new AuthException(AuthErrorCode.CODE_VERIFIER_REQUIRED, HttpStatus.BAD_REQUEST);
            }

            boolean isValidVerifier = PKCEUtils.verifyCodeChallenge(
                request.getCodeVerifier(),
                authCode.getCodeChallenge(),
                authCode.getCodeChallengeMethod()
            );

            if (!isValidVerifier) {
                throw new AuthException(AuthErrorCode.INVALID_CODE_VERIFIER, HttpStatus.BAD_REQUEST);
            }
        }

        try {
            String accessToken = jwtService.generateAccessToken(
                authCode.getUserId(),
                clientId,
                authCode.getScope()
            );

            String refreshToken = jwtService.generateRefreshToken(
                authCode.getUserId(),
                clientId,
                authCode.getScope()
            );

            String idToken = null;
            if (authCode.getScope() != null && authCode.getScope().contains("openid")) {
                idToken = jwtService.generateIdToken(
                    authCode.getUserId(),
                    clientId,
                    request.getNonce()
                );
            }

            TokenResponse response = new TokenResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setIdToken(idToken);
            response.setTokenType(TOKEN_TYPE);
            response.setExpiresIn(ACCESS_TOKEN_EXPIRES_IN);
            response.setScope(authCode.getScope());
            return response;

        } catch (Exception e) {
            log.error("Token generation failed", e);
            throw new AuthException(AuthErrorCode.TOKEN_GENERATE_ERROR);
        }
    }
    
    /**
     * 使用刷新令牌获取新的访问令牌
     */
    private TokenResponse refreshToken(String clientId, String clientSecret, TokenRequest request) {
        // 验证客户端
        RegisteredClient client = validateClient(clientId, clientSecret);

        try {
            // 验证刷新令牌
            Claims claims = jwtService.validateRefreshToken(request.getRefreshToken());
            
            String userId = claims.get("userId", String.class);
            String tokenClientId = claims.get("clientId", String.class);
            String scope = claims.get("scope", String.class);
            
            // 验证刷新令牌的客户端ID与当前客户端ID是否匹配
            if (!clientId.equals(tokenClientId)) {
                throw new AuthException(AuthErrorCode.INVALID_CLIENT);
            }

            String accessToken = jwtService.generateAccessToken(userId, clientId, scope);
            String refreshToken = jwtService.generateRefreshToken(userId, clientId, scope);

            // 如果scope包含openid，并且提供了nonce，则生成ID Token
            String idToken = null;
            if (scope != null && scope.contains("openid")) {
                idToken = jwtService.generateIdToken(userId, clientId, request.getNonce());
            }

            TokenResponse response = new TokenResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setIdToken(idToken);
            response.setTokenType(TOKEN_TYPE);
            response.setExpiresIn(ACCESS_TOKEN_EXPIRES_IN);
            response.setScope(scope);
            return response;

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
    }
    
    /**
     * 验证客户端凭证
     */
    private RegisteredClient validateClient(String clientId, String clientSecret) {
        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null || !passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            throw new AuthException(AuthErrorCode.INVALID_CLIENT_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
        return client;
    }
} 