package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.utils.PKCEUtils;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.Claims;

@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final CustomJdbcRegisteredClientRepository clientRepository;
    private final AuthorizationCodeService authorizationCodeService;
    private final JwtService jwtService;  // 我们需要创建这个服务来生成JWT令牌
    
    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String TOKEN_TYPE = "Bearer";
    private static final int ACCESS_TOKEN_EXPIRES_IN = 3600;  // 访问令牌1小时过期

    @Transactional
    public TokenResponse createToken(TokenRequest request) {
        return switch (request.getGrantType()) {
            case GRANT_TYPE_AUTHORIZATION_CODE -> createTokenByAuthorizationCode(request);
            case GRANT_TYPE_REFRESH_TOKEN -> refreshToken(request);
            default -> throw new AuthException(AuthErrorCode.INVALID_GRANT_TYPE);
        };
    }
    
    private TokenResponse createTokenByAuthorizationCode(TokenRequest request) {
        // 验证授权类型
        if (!GRANT_TYPE_AUTHORIZATION_CODE.equals(request.getGrantType())) {
            throw new AuthException(AuthErrorCode.INVALID_GRANT_TYPE);
        }

        // 验证客户端
        RegisteredClient client = clientRepository.findByClientId(request.getClientId());
        if (client == null || !client.getClientSecret().equals(request.getClientSecret())) {
            throw new AuthException(AuthErrorCode.INVALID_CLIENT_CREDENTIALS);
        }

        // 验证并消费授权码
        AuthorizationCode authCode = authorizationCodeService.validateAndConsume(
            request.getCode(),
            request.getClientId(),
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
                request.getClientId(),
                authCode.getScope()
            );

            // 生成刷新令牌
            String refreshToken = jwtService.generateRefreshToken(
                authCode.getUserId(),
                request.getClientId(),
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
    
    private TokenResponse refreshToken(TokenRequest request) {
        // 验证客户端
        RegisteredClient client = clientRepository.findByClientId(request.getClientId());
        if (client == null || !client.getClientSecret().equals(request.getClientSecret())) {
            throw new AuthException(AuthErrorCode.INVALID_CLIENT_CREDENTIALS);
        }

        try {
            // 验证刷新令牌
            Claims claims = jwtService.parseToken(request.getRefreshToken());
            if (!"refresh_token".equals(claims.get("type", String.class))) {
                throw new AuthException(AuthErrorCode.TOKEN_INVALID);
            }

            String userId = claims.get("userId", String.class);
            String clientId = claims.get("clientId", String.class);
            String scope = claims.get("scope", String.class);

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
} 