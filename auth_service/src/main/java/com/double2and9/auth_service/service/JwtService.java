package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.TokenIntrospectionResponse;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.security.JwtTokenProvider;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    
    private static final int ACCESS_TOKEN_EXPIRES_IN = 3600;  // 1小时
    private static final int REFRESH_TOKEN_EXPIRES_IN = 2592000;  // 30天

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(String userId, String clientId, String scope) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("clientId", clientId);
        claims.put("scope", scope);
        claims.put("type", "access_token");

        return jwtTokenProvider.generateToken(claims, (long) ACCESS_TOKEN_EXPIRES_IN);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String userId, String clientId, String scope) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("clientId", clientId);
        claims.put("scope", scope);
        claims.put("type", "refresh_token");

        return jwtTokenProvider.generateToken(claims, (long) REFRESH_TOKEN_EXPIRES_IN);
    }

    /**
     * 验证访问令牌
     */
    public Claims validateAccessToken(String token) {
        Claims claims = jwtTokenProvider.validateToken(token);
        
        // 验证令牌类型
        String type = claims.get("type", String.class);
        if (!"access_token".equals(type)) {
            throw new IllegalArgumentException("Invalid token type");
        }

        return claims;
    }

    /**
     * 验证并解析刷新令牌
     * @param refreshToken 刷新令牌
     * @return 令牌中的声明
     * @throws AuthException 如果令牌无效或已过期
     */
    public Claims validateRefreshToken(String refreshToken) {
        Claims claims = parseToken(refreshToken);
        
        // 验证令牌类型
        String tokenType = claims.get("type", String.class);
        if (!"refresh_token".equals(tokenType)) {
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
        
        return claims;
    }

    /**
     * 从令牌中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = jwtTokenProvider.validateToken(token);
        return claims.get("userId", String.class);
    }

    /**
     * 从令牌中获取客户端ID
     */
    public String getClientIdFromToken(String token) {
        Claims claims = jwtTokenProvider.validateToken(token);
        return claims.get("clientId", String.class);
    }

    /**
     * 从访问令牌中获取授权范围
     */
    public String getScopeFromToken(String token) {
        Claims claims = jwtTokenProvider.validateToken(token);
        return claims.get("scope", String.class);
    }

    /**
     * 撤销令牌
     */
    public void revokeToken(String token) {
        try {
            Claims claims = jwtTokenProvider.validateToken(token);
            // 获取令牌过期时间
            long expirationTime = claims.getExpiration().getTime() / 1000 - System.currentTimeMillis() / 1000;
            if (expirationTime > 0) {
                tokenBlacklistService.addToBlacklist(token, expirationTime);
            }
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 解析令牌
     */
    public Claims parseToken(String token) {
        try {
            // 先验证令牌格式
            log.debug("Validating token");
            Claims claims = jwtTokenProvider.validateToken(token);
            
            // 检查令牌是否被撤销
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.debug("Token is blacklisted");
                throw new AuthException(AuthErrorCode.TOKEN_REVOKED);
            }
            log.debug("Token validation successful");
            return claims;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired");
            throw new AuthException(AuthErrorCode.TOKEN_EXPIRED);
        } catch (SignatureException e) {
            log.debug("Invalid token signature");
            throw new AuthException(AuthErrorCode.TOKEN_SIGNATURE_INVALID);
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported token format");
            throw new AuthException(AuthErrorCode.TOKEN_UNSUPPORTED);
        } catch (AuthException e) {
            // 直接抛出 AuthException，不做转换
            log.debug("Auth exception while parsing token: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.debug("Unexpected error while parsing token: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 内省令牌
     */
    public TokenIntrospectionResponse introspectToken(String token) {
        log.debug("Introspecting token: {}", token);
        try {
            // 检查令牌是否在黑名单中
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.debug("Token is blacklisted");
                return TokenIntrospectionResponse.builder()
                    .active(false)
                    .build();
            }

            // 验证并解析令牌
            log.debug("Attempting to parse token");
            Claims claims = jwtTokenProvider.validateToken(token);
            log.debug("Token parsed successfully. Claims: {}", claims);
            
            // 只有当令牌完全有效时才返回 active=true
            return TokenIntrospectionResponse.builder()
                .active(true)
                .clientId(claims.get("clientId", String.class))
                .userId(claims.get("userId", String.class))
                .scope(claims.get("scope", String.class))
                .exp(claims.getExpiration().getTime() / 1000)
                .iat(claims.getIssuedAt().getTime() / 1000)
                .tokenType(claims.get("type", String.class))
                .build();

        } catch (AuthException e) {
            log.debug("Token introspection failed with AuthException: {}", e.getMessage(), e);
            return TokenIntrospectionResponse.builder()
                .active(false)
                .build();
        } catch (Exception e) {
            log.error("Token introspection failed with unexpected error: {}", e.getMessage(), e);
            return TokenIntrospectionResponse.builder()
                .active(false)
                .build();
        }
    }

    /**
     * 使用刷新令牌生成新的访问令牌和刷新令牌
     * @param refreshToken 原刷新令牌
     * @return 新的令牌对
     */
    public TokenResponse refreshTokens(String refreshToken) {
        Claims claims = validateRefreshToken(refreshToken);
        
        String userId = claims.get("userId", String.class);
        String clientId = claims.get("clientId", String.class);
        String scope = claims.get("scope", String.class);

        // 生成新的访问令牌和刷新令牌
        String newAccessToken = generateAccessToken(userId, clientId, scope);
        String newRefreshToken = generateRefreshToken(userId, clientId, scope);

        TokenResponse response = new TokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(ACCESS_TOKEN_EXPIRES_IN);
        response.setScope(scope);

        return response;
    }
} 