package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.utils.PKCEUtils;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Claims;

@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final CustomJdbcRegisteredClientRepository clientRepository;
    private final AuthorizationCodeService authorizationCodeService;
    private final JwtService jwtService;
    
    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String TOKEN_TYPE = "Bearer";
    private static final int ACCESS_TOKEN_EXPIRES_IN = 3600;  // 访问令牌1小时过期

    /**
     * 创建令牌
     * 
     * @param clientId 客户端ID（来自HTTP Basic认证）
     * @param clientSecret 客户端密钥（来自HTTP Basic认证）
     * @param request 令牌请求
     * @return 令牌响应
     */
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
                throw new AuthException(AuthErrorCode.CODE_VERIFIER_REQUIRED);
            }

            boolean isValidVerifier = PKCEUtils.verifyCodeChallenge(
                request.getCodeVerifier(),
                authCode.getCodeChallenge(),
                authCode.getCodeChallengeMethod()
            );

            if (!isValidVerifier) {
                throw new AuthException(AuthErrorCode.INVALID_CODE_VERIFIER);
            }
        }

        try {
            // 生成访问令牌
            String accessToken = jwtService.generateAccessToken(
                authCode.getUserId(),
                clientId,
                authCode.getScope()
            );

            // 生成刷新令牌
            String refreshToken = jwtService.generateRefreshToken(
                authCode.getUserId(),
                clientId,
                authCode.getScope()
            );

            // 创建响应
            TokenResponse response = new TokenResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType(TOKEN_TYPE);
            response.setExpiresIn(ACCESS_TOKEN_EXPIRES_IN);
            response.setScope(authCode.getScope());

            return response;
        } catch (Exception e) {
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
            Claims claims = jwtService.parseToken(request.getRefreshToken());
            if (!"refresh_token".equals(claims.get("type", String.class))) {
                throw new AuthException(AuthErrorCode.TOKEN_INVALID);
            }

            String userId = claims.get("userId", String.class);
            String tokenClientId = claims.get("clientId", String.class);
            String scope = claims.get("scope", String.class);
            
            // 验证刷新令牌的客户端ID与当前客户端ID是否匹配
            if (!clientId.equals(tokenClientId)) {
                throw new AuthException(AuthErrorCode.INVALID_CLIENT);
            }

            // 生成新的访问令牌和刷新令牌
            String accessToken = jwtService.generateAccessToken(userId, clientId, scope);
            String refreshToken = jwtService.generateRefreshToken(userId, clientId, scope);

            // 创建响应
            TokenResponse response = new TokenResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType(TOKEN_TYPE);
            response.setExpiresIn(ACCESS_TOKEN_EXPIRES_IN);
            response.setScope(scope);

            return response;
        } catch (Exception e) {
            // 如果是 AuthException，直接抛出
            if (e instanceof AuthException) {
                throw (AuthException) e;
            }
            // 其他异常转换为 TOKEN_INVALID
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
    }
    
    /**
     * 验证客户端凭证
     * 
     * @param clientId 客户端ID
     * @param clientSecret 客户端密钥
     * @return 客户端
     * @throws AuthException 如果客户端验证失败
     */
    private RegisteredClient validateClient(String clientId, String clientSecret) {
        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null || !client.getClientSecret().equals(clientSecret)) {
            throw new AuthException(AuthErrorCode.INVALID_CLIENT_CREDENTIALS);
        }
        return client;
    }
} 