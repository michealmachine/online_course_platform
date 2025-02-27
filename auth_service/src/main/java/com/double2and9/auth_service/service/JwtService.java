package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.TokenIntrospectionResponse;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.security.AuthJwtTokenProvider;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.double2and9.auth_service.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class JwtService {
    
    private final AuthJwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    
    private static final int ACCESS_TOKEN_EXPIRES_IN = 3600;  // 1小时
    private static final int REFRESH_TOKEN_EXPIRES_IN = 2592000;  // 30天

    /**
     * 生成JWT令牌
     * @param claims 令牌中的声明
     * @param expirationSeconds 过期时间（秒）
     * @return JWT令牌
     */
    public String generateToken(Map<String, Object> claims, long expirationSeconds) {
        return jwtTokenProvider.generateToken(claims, expirationSeconds);
    }

    /**
     * 生成访问令牌
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param scope 授权范围
     * @return 访问令牌
     */
    public String generateAccessToken(String userId, String clientId, String scope) {
        try {
            Long userIdLong = Long.valueOf(userId);
            // 查找用户信息以获取机构ID
            User user = userRepository.findById(userIdLong)
                    .orElseThrow(() -> {
                        log.error("User not found with ID: {}", userId);
                        return new AuthException(AuthErrorCode.USER_NOT_FOUND);
                    });

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("clientId", clientId);
            claims.put("scope", scope);
            claims.put("type", "access_token");
            
            // 如果是机构用户，添加机构ID
            if (user.getOrganizationId() != null) {
                claims.put("organization_id", user.getOrganizationId());
            }
            
            return generateToken(claims, ACCESS_TOKEN_EXPIRES_IN);
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to generate access token for user {}: {}", userId, e.getMessage());
            throw new AuthException(AuthErrorCode.TOKEN_GENERATE_ERROR);
        }
    }

    /**
     * 生成刷新令牌
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param scope 授权范围
     * @return 刷新令牌
     */
    public String generateRefreshToken(String userId, String clientId, String scope) {
        try {
            Long userIdLong = Long.valueOf(userId);
            // 查找用户信息以获取机构ID
            User user = userRepository.findById(userIdLong)
                    .orElseThrow(() -> {
                        log.error("User not found with ID: {}", userId);
                        return new AuthException(AuthErrorCode.USER_NOT_FOUND);
                    });

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("clientId", clientId);
            claims.put("scope", scope);
            claims.put("type", "refresh_token");
            
            // 如果是机构用户，添加机构ID
            if (user.getOrganizationId() != null) {
                claims.put("organization_id", user.getOrganizationId());
            }
            
            return generateToken(claims, REFRESH_TOKEN_EXPIRES_IN);
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to generate refresh token for user {}: {}", userId, e.getMessage());
            throw new AuthException(AuthErrorCode.TOKEN_GENERATE_ERROR);
        }
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

    /**
     * 生成ID令牌
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param nonce 随机数
     * @return ID令牌
     */
    public String generateIdToken(String userId, String clientId, String nonce) {
        try {
            // 查找用户信息
            User user = userRepository.findById(Long.valueOf(userId))
                    .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

            // 构建 ID Token claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", userId);  // 保持一致性，使用传入的userId
            claims.put("aud", clientId);
            claims.put("name", user.getUsername());
            claims.put("email", user.getEmail());
            claims.put("email_verified", user.getEmailVerified());
            claims.put("preferred_username", user.getPreferredUsername());
            claims.put("given_name", user.getGivenName());
            claims.put("middle_name", user.getMiddleName());
            claims.put("family_name", user.getFamilyName());
            claims.put("nickname", user.getNickname());
            claims.put("profile", user.getProfile());
            claims.put("picture", user.getAvatar());
            claims.put("website", user.getWebsite());
            claims.put("gender", user.getGender());
            claims.put("birthdate", user.getBirthdate());
            claims.put("zoneinfo", user.getZoneinfo());
            claims.put("locale", user.getLocale());
            claims.put("phone_number", user.getPhone());
            claims.put("phone_number_verified", user.getPhoneVerified());

            // 如果是机构用户，添加机构ID
            if (user.getOrganizationId() != null) {
                claims.put("organization_id", user.getOrganizationId());
            }

            // 如果提供了nonce，添加到claims中
            if (nonce != null) {
                claims.put("nonce", nonce);
            }

            // 生成ID Token
            return generateToken(claims, 3600L);
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        }
    }

    public TokenIntrospectionResponse introspectIdToken(String token) {
        try {
            // 检查令牌是否在黑名单中
            if (tokenBlacklistService.isBlacklisted(token)) {
                return TokenIntrospectionResponse.builder()
                    .active(false)
                    .build();
            }

            Claims claims = jwtTokenProvider.validateToken(token);
            TokenIntrospectionResponse response = new TokenIntrospectionResponse();
            response.setActive(true);
            response.setUserId(claims.get("sub", String.class));  // ID Token 使用 sub 作为用户ID
            response.setClientId(claims.get("aud", String.class));  // ID Token 使用 aud 作为客户端ID
            response.setScope("openid profile email");  // ID Token 的默认作用域
            response.setExp(claims.getExpiration().getTime() / 1000);  // 转换为秒
            response.setIat(claims.getIssuedAt().getTime() / 1000);  // 转换为秒
            response.setTokenType("id_token");
            return response;
        } catch (Exception e) {
            // 如果令牌无效或已过期，返回 active=false
            return TokenIntrospectionResponse.builder()
                .active(false)
                .build();
        }
    }
} 