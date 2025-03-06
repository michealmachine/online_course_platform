package com.double2and9.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.double2and9.auth_service.service.TokenBlacklistService;
import com.double2and9.base.enums.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthJwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            if (jwt != null) {
                log.debug("JWT token from request: {}", jwt);

                // 首先检查令牌是否被撤销
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    log.debug("Token is blacklisted: {}", jwt);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("code", AuthErrorCode.TOKEN_REVOKED.getCode());
                    errorResponse.put("message", AuthErrorCode.TOKEN_REVOKED.getMessage());
                    
                    objectMapper.writeValue(response.getWriter(), errorResponse);
                    return;
                }

                if (jwtTokenProvider.isTokenValid(jwt)) {
                    log.debug("JWT token is valid");
                    var claims = jwtTokenProvider.parseToken(jwt);
                    
                    // 优先使用 sub 字段，如果没有再使用 userId
                    String userId = claims.getSubject();
                    if (userId == null) {
                        userId = claims.get("userId", String.class);
                    }
                    log.debug("User ID from token: {}", userId);
                    
                    if (userId != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                        log.debug("User details loaded for user ID: {}", userId);
                        
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, jwt, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authentication set in SecurityContext for user ID: {}", userId);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 检查请求路径是否是OAuth2相关端点
        boolean isOAuth2Endpoint = path.startsWith("/oauth2/") 
            || path.equals("/oauth2/authorize")
            || path.equals("/oauth2/token")
            || path.equals("/oauth2/introspect")
            || path.equals("/oauth2/revoke")
            || path.equals("/oauth2/userinfo")
            || path.startsWith("/.well-known/");
            
        // 检查请求路径是否是认证相关页面
        boolean isAuthPage = path.startsWith("/auth/login") 
            || path.startsWith("/auth/register")
            || path.equals("/auth/captcha")
            || path.equals("/error");
            
        // 检查请求路径是否是静态资源
        boolean isStaticResource = path.startsWith("/css/") 
            || path.startsWith("/js/") 
            || path.startsWith("/images/") 
            || path.startsWith("/webjars/")
            || path.equals("/favicon.ico");
            
        // 检查Accept或Content-Type是否表明这是一个认证API请求
        // 如果请求期望返回JSON或提交JSON，可能是API调用而非页面访问
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        boolean isApiCall = (acceptHeader != null && acceptHeader.contains("application/json"))
            || (contentType != null && contentType.contains("application/json"));
            
        // 对于OAuth2端点、认证页面和静态资源，不应用JWT过滤器
        // 对于API请求或期望JSON响应的请求，应用JWT过滤器
        boolean shouldSkip = isOAuth2Endpoint || isAuthPage || isStaticResource;
        
        // 为特定API请求启用JWT认证，即使它们在OAuth2端点下
        if (isOAuth2Endpoint && isApiCall && path.startsWith("/api/")) {
            return false; // 不跳过过滤，应用JWT认证
        }
        
        return shouldSkip;
    }
} 