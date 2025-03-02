package com.double2and9.auth_service.service.impl;

import com.double2and9.auth_service.dto.request.OAuth2AuthorizationRequest;
import com.double2and9.auth_service.service.AuthorizationRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于会话的授权请求存储服务实现
 */
@Service
public class SessionAuthorizationRequestService implements AuthorizationRequestService {

    private static final String OAUTH2_AUTHORIZATION_REQUEST_SESSION_KEY = "oauth2_auth_request";

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        
        HttpSession session = request.getSession(true);
        session.setAttribute(OAUTH2_AUTHORIZATION_REQUEST_SESSION_KEY, authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest getAuthorizationRequest(HttpServletRequest request,
                                                           HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        
        return (OAuth2AuthorizationRequest) session.getAttribute(OAUTH2_AUTHORIZATION_REQUEST_SESSION_KEY);
    }

    @Override
    public void removeAuthorizationRequest(HttpServletRequest request,
                                         HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(OAUTH2_AUTHORIZATION_REQUEST_SESSION_KEY);
        }
    }

    @Override
    public OAuth2AuthorizationRequest extractAuthorizationRequest(HttpServletRequest request) {
        String clientId = request.getParameter("client_id");
        
        // 如果没有客户端ID，则不是授权请求
        if (!StringUtils.hasText(clientId)) {
            return null;
        }
        
        return OAuth2AuthorizationRequest.builder()
                .clientId(clientId)
                .scope(request.getParameter("scope"))
                .state(request.getParameter("state"))
                .redirectUri(request.getParameter("redirect_uri"))
                .responseType(request.getParameter("response_type"))
                .codeChallenge(request.getParameter("code_challenge"))
                .codeChallengeMethod(request.getParameter("code_challenge_method"))
                .nonce(request.getParameter("nonce"))
                .continueAuthorization(Boolean.parseBoolean(request.getParameter("continue_authorization")))
                .build();
    }
} 