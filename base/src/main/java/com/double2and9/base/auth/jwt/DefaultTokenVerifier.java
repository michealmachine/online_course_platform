package com.double2and9.base.auth.jwt;

import com.double2and9.base.auth.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认令牌验证实现
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultTokenVerifier implements TokenVerifier {
    
    private final JwtProperties jwtProperties;
    
    @Override
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        return JwtUtils.validateToken(token, jwtProperties.getSecret());
    }
    
    @Override
    public String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    @Override
    public String getClientIdFromToken(String token, String clientIdClaimName) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        if (clientIdClaimName == null || clientIdClaimName.trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID claim name cannot be null or empty");
        }
        
        Claims claims = parseToken(token);
        return (String) claims.get(clientIdClaimName);
    }
} 