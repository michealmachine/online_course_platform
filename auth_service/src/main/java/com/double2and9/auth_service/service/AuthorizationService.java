package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.AuthorizationRequest;
import com.double2and9.auth_service.dto.response.AuthorizationResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorizationService {
    
    private final CustomJdbcRegisteredClientRepository clientRepository;
    private final AuthorizationConsentService authorizationConsentService;

    @Transactional
    public AuthorizationResponse createAuthorizationRequest(AuthorizationRequest request, Authentication authentication) {
        // 验证用户是否已登录
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        // 验证响应类型
        if (!"code".equals(request.getResponseType())) {
            throw new AuthException(AuthErrorCode.RESPONSE_TYPE_INVALID);
        }

        // 获取客户端信息
        RegisteredClient client = clientRepository.findByClientId(request.getClientId());
        if (client == null) {
            throw new AuthException(AuthErrorCode.CLIENT_NOT_FOUND);
        }

        // 验证重定向URI
        if (!client.getRedirectUris().contains(request.getRedirectUri())) {
            throw new AuthException(AuthErrorCode.CLIENT_REDIRECT_URI_INVALID);
        }

        // 验证授权范围
        Set<String> requestedScopes = Arrays.stream(request.getScope().split("\\s+"))
                .collect(Collectors.toSet());
        if (!client.getScopes().containsAll(requestedScopes)) {
            throw new AuthException(AuthErrorCode.CLIENT_SCOPE_INVALID);
        }

        // 创建授权响应
        AuthorizationResponse response = new AuthorizationResponse();
        response.setClientId(client.getClientId());
        response.setClientName(client.getClientName());
        response.setRequestedScopes(requestedScopes);
        response.setState(request.getState());
        response.setAuthorizationId(UUID.randomUUID().toString());
        response.setRedirectUri(request.getRedirectUri());

        // 保存授权请求
        authorizationConsentService.savePendingAuthorization(response.getAuthorizationId(), response);

        return response;
    }
} 