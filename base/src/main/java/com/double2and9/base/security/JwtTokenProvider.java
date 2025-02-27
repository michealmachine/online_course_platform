package com.double2and9.base.security;

import com.double2and9.base.config.JwtProperties;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final Key signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = initializeSigningKey();
    }

    private Key initializeSigningKey() {
        try {
            String secret = jwtProperties.getSecret();
            if (secret == null || secret.trim().isEmpty()) {
                log.info("No JWT secret configured, generating a secure key");
                return Keys.secretKeyFor(SignatureAlgorithm.HS512);
            }

            byte[] keyBytes = Decoders.BASE64URL.decode(secret);
            Key key = Keys.hmacShaKeyFor(keyBytes);
            // 验证密钥是否足够安全
            SignatureAlgorithm.HS512.assertValidSigningKey(key);
            return key;
        } catch (WeakKeyException e) {
            log.warn("Configured JWT secret key is not secure enough for HS512, generating a secure key");
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        } catch (Exception e) {
            log.error("Error initializing JWT signing key, falling back to generating a secure key", e);
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
    }

    /**
     * 生成令牌
     */
    public String generateToken(Map<String, Object> claims, long expirationSeconds) {
        log.debug("Generating token with claims: {} and expiration: {}", claims, expirationSeconds);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证令牌
     */
    public Claims validateToken(String token) {
        log.debug("Validating token: {}", token);
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            log.debug("Token validation successful, claims: {}", claims);
            return claims;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired");
            throw new RuntimeException(AuthErrorCode.TOKEN_EXPIRED.getMessage());
        } catch (SignatureException e) {
            log.debug("Invalid token signature");
            throw new RuntimeException(AuthErrorCode.TOKEN_SIGNATURE_INVALID.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported token format");
            throw new RuntimeException(AuthErrorCode.TOKEN_UNSUPPORTED.getMessage());
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            throw new RuntimeException(AuthErrorCode.TOKEN_INVALID.getMessage());
        }
    }

    /**
     * 解析令牌
     */
    public Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException(AuthErrorCode.TOKEN_INVALID.getMessage());
        }

        try {
            return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException(AuthErrorCode.TOKEN_EXPIRED.getMessage());
        } catch (SignatureException e) {
            throw new RuntimeException(AuthErrorCode.TOKEN_SIGNATURE_INVALID.getMessage());
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException(AuthErrorCode.TOKEN_UNSUPPORTED.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(AuthErrorCode.TOKEN_INVALID.getMessage());
        }
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * 生成用户认证令牌
     */
    public String generateToken(Authentication authentication) {
        throw new UnsupportedOperationException("This method should be implemented by subclasses");
    }

    /**
     * 获取签名密钥
     */
    protected Key getSigningKey() {
        return signingKey;
    }

    /**
     * 获取JWT配置
     */
    protected JwtProperties getJwtProperties() {
        return jwtProperties;
    }

    /**
     * 验证令牌是否有效（不抛出异常）
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}