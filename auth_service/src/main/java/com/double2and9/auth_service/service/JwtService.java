package com.double2and9.auth_service.service;

import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.security.JwtTokenProvider;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    private static final long ACCESS_TOKEN_EXPIRES_IN = 3600L;  // 1小时
    private static final long REFRESH_TOKEN_EXPIRES_IN = 2592000L;  // 30天

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(String userId, String clientId, String scope) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("clientId", clientId);
        claims.put("scope", scope);
        claims.put("type", "access_token");

        return jwtTokenProvider.generateToken(claims, ACCESS_TOKEN_EXPIRES_IN);
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

        return jwtTokenProvider.generateToken(claims, REFRESH_TOKEN_EXPIRES_IN);
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
     * 验证刷新令牌
     */
    public Claims validateRefreshToken(String token) {
        Claims claims = jwtTokenProvider.validateToken(token);
        
        // 验证令牌类型
        String type = claims.get("type", String.class);
        if (!"refresh_token".equals(type)) {
            throw new IllegalArgumentException("Invalid token type");
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
     * 解析令牌
     */
    public Claims parseToken(String token) {
        try {
            return jwtTokenProvider.validateToken(token);
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.TOKEN_EXPIRED);
        } catch (SignatureException e) {
            throw new AuthException(AuthErrorCode.TOKEN_SIGNATURE_INVALID);
        } catch (UnsupportedJwtException e) {
            throw new AuthException(AuthErrorCode.TOKEN_UNSUPPORTED);
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
    }
} 