package com.double2and9.auth_service.security;

import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.*;
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

    @Value("${jwt.secret:defaultSecretKeydefaultSecretKeydefaultSecretKey}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 生成用户认证令牌
     */
    public String generateToken(Authentication authentication) {
        SecurityUser userPrincipal = (SecurityUser) authentication.getPrincipal();
        return generateToken(Map.of("sub", userPrincipal.getUsername()), jwtExpirationMs / 1000L);
    }

    /**
     * 生成自定义令牌
     */
    public String generateToken(Map<String, Object> claims, long expirationSeconds) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * 验证令牌并返回声明
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
            throw new AuthException(AuthErrorCode.TOKEN_SIGNATURE_INVALID);
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
            throw new AuthException(AuthErrorCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
            throw new AuthException(AuthErrorCode.TOKEN_UNSUPPORTED);
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
            throw new AuthException(AuthErrorCode.TOKEN_CLAIMS_EMPTY);
        }
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