package com.double2and9.auth_service.dto.request;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

/**
 * OAuth2授权请求参数DTO
 * 用于在登录过程中保存授权请求参数，实现授权流程与登录的无缝衔接
 */
@Data
@Builder
public class OAuth2AuthorizationRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 权限范围，多个范围用空格分隔
     */
    private String scope;
    
    /**
     * 客户端状态参数，用于防止CSRF攻击
     */
    private String state;
    
    /**
     * 授权成功后的重定向URI
     */
    private String redirectUri;
    
    /**
     * 响应类型，OAuth2授权码模式为"code"
     */
    private String responseType;
    
    /**
     * PKCE挑战码
     */
    private String codeChallenge;
    
    /**
     * PKCE挑战码生成方法，通常为"S256"
     */
    private String codeChallengeMethod;
    
    /**
     * OpenID Connect的nonce参数，用于防止重放攻击
     */
    private String nonce;
    
    /**
     * 是否继续授权流程的标志
     */
    private boolean continueAuthorization;
    
    /**
     * 构建授权请求URL
     * @return 完整的授权请求URL
     */
    public String buildAuthorizationRequestUrl() {
        StringBuilder urlBuilder = new StringBuilder("/oauth2/authorize?");
        
        if (clientId != null) {
            urlBuilder.append("client_id=").append(clientId).append("&");
        }
        
        if (scope != null) {
            urlBuilder.append("scope=").append(scope).append("&");
        }
        
        if (state != null) {
            urlBuilder.append("state=").append(state).append("&");
        }
        
        if (redirectUri != null) {
            urlBuilder.append("redirect_uri=").append(redirectUri).append("&");
        }
        
        if (responseType != null) {
            urlBuilder.append("response_type=").append(responseType).append("&");
        }
        
        if (codeChallenge != null) {
            urlBuilder.append("code_challenge=").append(codeChallenge).append("&");
        }
        
        if (codeChallengeMethod != null) {
            urlBuilder.append("code_challenge_method=").append(codeChallengeMethod).append("&");
        }
        
        if (nonce != null) {
            urlBuilder.append("nonce=").append(nonce).append("&");
        }
        
        // 移除最后一个"&"字符
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '&') {
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }
        
        return urlBuilder.toString();
    }
} 