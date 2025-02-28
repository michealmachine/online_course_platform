package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.UserInfoResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class OidcSessionService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtService jwtService;
    private final UserService userService;
    
    // 会话管理相关的Redis key前缀
    private static final String SESSION_PREFIX = "oidc:session:";
    private static final String CHECK_SESSION_PREFIX = "oidc:check_session:";
    
    /**
     * 处理用户登出
     */
    public void endSession(String idTokenHint, String postLogoutRedirectUri, String state) {
        // 验证ID Token
        if (idTokenHint != null) {
            Claims claims = jwtService.parseToken(idTokenHint);
            // 验证postLogoutRedirectUri是否匹配客户端配置
            String clientId = claims.get("aud", String.class);
            // TODO: 验证重定向URI
        }
        
        // 清理会话状态
        clearSession(idTokenHint);
    }
    
    /**
     * 检查会话状态
     */
    public boolean checkSession(String clientId, String sessionState) {
        String key = CHECK_SESSION_PREFIX + clientId + ":" + sessionState;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * 处理RP发起的登出
     */
    public String handleRpInitiatedLogout(String idTokenHint, String postLogoutRedirectUri, String state) {
        // 如果提供了ID Token，则验证并清理会话
        if (idTokenHint != null) {
            Claims claims = jwtService.parseToken(idTokenHint);
            
            // 获取用户信息
            String userId = claims.getSubject();
            UserInfoResponse userInfo = userService.getUserInfo(Long.valueOf(userId));
            
            // 清理会话
            clearSession(idTokenHint);
        }
        
        // 返回登出重定向URI
        return buildLogoutRedirectUri(postLogoutRedirectUri, state);
    }
    
    private void clearSession(String idToken) {
        if (idToken != null) {
            Claims claims = jwtService.parseToken(idToken);
            String sessionKey = SESSION_PREFIX + claims.getSubject();
            redisTemplate.delete(sessionKey);
        }
    }
    
    private String buildLogoutRedirectUri(String baseUri, String state) {
        if (baseUri == null) {
            return null;
        }
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUri);
        if (state != null) {
            builder.queryParam("state", state);
        }
        return builder.build().toUriString();
    }
} 