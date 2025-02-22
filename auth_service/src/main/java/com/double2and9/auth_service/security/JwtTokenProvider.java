package com.double2and9.auth_service.security;

import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationMs;

    /**
     * 生成令牌
     */
    public String generateToken(Map<String, Object> claims, long expirationSeconds) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证令牌
     */
    public Claims validateToken(String token) {
        log.debug("Validating token in JwtTokenProvider: {}", token);
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            log.debug("Token validation successful, claims: {}", claims);
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
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 解析令牌
     */
    public Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }

        try {
            return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成用户认证令牌
     */
    public String generateToken(Authentication authentication) {
        SecurityUser userPrincipal = (SecurityUser) authentication.getPrincipal();
        return generateToken(Map.of("sub", userPrincipal.getUsername()), jwtExpirationMs / 1000L);
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * 验证令牌是否有效（不抛出异常）
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (AuthException e) {
            return false;
        }
    }
} 