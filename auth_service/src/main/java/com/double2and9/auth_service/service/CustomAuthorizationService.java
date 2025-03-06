package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.AuthorizationRequest;
import com.double2and9.auth_service.dto.request.ConsentRequest;
import com.double2and9.auth_service.dto.response.AuthorizationResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 自定义授权服务
 * 
 * @deprecated 此类已被标准的Spring Security OAuth2授权服务器实现所取代。
 * 不再需要自定义授权控制器和服务，请使用Spring Security提供的标准OAuth2实现。
 * 此类仅为保持测试兼容性而保留。
 */
@Slf4j
@Service
@Deprecated
@RequiredArgsConstructor
public class CustomAuthorizationService {
    
    private final CustomJdbcRegisteredClientRepository clientRepository;
    private final CustomAuthorizationConsentService authorizationConsentService;
    private final ClientService clientService;

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

        // 验证PKCE参数
        if (request.getCodeChallenge() == null || request.getCodeChallengeMethod() == null) {
            throw new AuthException(AuthErrorCode.PKCE_REQUIRED, HttpStatus.BAD_REQUEST);
        }

        // 验证code_challenge_method
        if (!("plain".equals(request.getCodeChallengeMethod()) || 
              "S256".equals(request.getCodeChallengeMethod()))) {
            throw new AuthException(AuthErrorCode.INVALID_CODE_CHALLENGE_METHOD, HttpStatus.BAD_REQUEST);
        }

        // 验证code_challenge格式
        if (!request.getCodeChallenge().matches("^[A-Za-z0-9\\-._~]{43,128}$")) {
            throw new AuthException(AuthErrorCode.INVALID_CODE_CHALLENGE, HttpStatus.BAD_REQUEST);
        }

        // 创建授权响应
        AuthorizationResponse response = new AuthorizationResponse();
        response.setClientId(client.getClientId());
        response.setClientName(client.getClientName());
        response.setRequestedScopes(requestedScopes);
        response.setState(request.getState());
        response.setAuthorizationId(UUID.randomUUID().toString());
        response.setRedirectUri(request.getRedirectUri());
        
        // 保存PKCE参数到Redis中的授权请求
        response.setCodeChallenge(request.getCodeChallenge());
        response.setCodeChallengeMethod(request.getCodeChallengeMethod());

        // 检查是否为内部客户端并且允许自动授权
        boolean isInternalClient = clientService.isInternalClient(client.getClientId());
        boolean isAutoApproveClient = clientService.isAutoApproveClient(client.getClientId());
        
        // 如果是内部客户端且允许自动授权，则自动创建授权码
        if (isInternalClient && isAutoApproveClient) {
            log.info("内部客户端自动授权: clientId={}, userId={}", client.getClientId(), authentication.getName());
            
            // 创建自动授权同意请求
            ConsentRequest consentRequest = new ConsentRequest();
            consentRequest.setClientId(client.getClientId());
            consentRequest.setUserId(authentication.getName());
            consentRequest.setPrincipal(authentication.getName());
            consentRequest.setRedirectUri(request.getRedirectUri());
            consentRequest.setScopes(requestedScopes);
            
            // 自动生成授权码
            String authorizationCode = authorizationConsentService.consent(consentRequest);
            
            // 设置授权码到响应中
            response.setAuthorizationCode(authorizationCode);
            
            log.info("内部客户端自动授权完成: clientId={}, userId={}", client.getClientId(), authentication.getName());
        } else {
            // 普通授权流程，保存授权请求等待用户确认
            authorizationConsentService.savePendingAuthorization(response.getAuthorizationId(), response);
        }

        return response;
    }
} 