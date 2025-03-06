package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.AuthorizationConsentRequest;
import com.double2and9.auth_service.dto.request.ConsentRequest;
import com.double2and9.auth_service.dto.response.AuthorizationConsentResponse;
import com.double2and9.auth_service.dto.response.AuthorizationResponse;
import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.double2and9.auth_service.utils.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * 自定义授权确认服务
 * 
 * @deprecated 此类已被标准的Spring Security OAuth2授权服务器实现所取代。
 * 不再需要自定义授权控制器和服务，请使用Spring Security提供的标准OAuth2实现。
 * 此类仅为保持测试兼容性而保留。
 */
@Slf4j
@Service
@Deprecated
@RequiredArgsConstructor
public class CustomAuthorizationConsentService {
    
    private final RegisteredClientRepository clientRepository;
    private final OAuth2AuthorizationService oauth2AuthorizationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthorizationCodeService authorizationCodeService;
    private final AuthorizationCodeRepository authorizationCodeRepository;
    private final OAuth2AuthorizationConsentService authorizationConsentService;
    private final OAuth2AuthorizationService authorizationService;
    
    private static final String REDIS_KEY_PREFIX = "oauth2:auth:request:";
    private static final long AUTHORIZATION_REQUEST_TIMEOUT = 10; // 10分钟过期

    /**
     * 获取授权请求信息，用于显示同意页面
     *
     * @param authorizationId 授权请求ID
     * @param authentication 当前用户认证信息
     * @return 授权请求信息
     */
    public AuthorizationResponse getAuthorizationRequest(String authorizationId, Authentication authentication) {
        // 验证用户是否已认证
        if (!authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }

        // 从Redis获取授权请求
        String authorizationRequestKey = REDIS_KEY_PREFIX + authorizationId;
        AuthorizationResponse authorizationResponse = (AuthorizationResponse) redisTemplate.opsForValue().get(authorizationRequestKey);
        if (authorizationResponse == null) {
            throw new AuthException(AuthErrorCode.AUTHORIZATION_REQUEST_NOT_FOUND);
        }

        // 验证客户端
        RegisteredClient client = clientRepository.findByClientId(authorizationResponse.getClientId());
        if (client == null) {
            throw new AuthException(AuthErrorCode.CLIENT_NOT_FOUND);
        }

        return authorizationResponse;
    }

    /**
     * 处理用户对授权请求的同意操作
     */
    @Transactional
    public AuthorizationConsentResponse consent(AuthorizationConsentRequest request, Authentication authentication) {
        // 验证用户是否已认证
        if (!authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }

        // 从 Redis 获取授权请求
        String authorizationRequestKey = REDIS_KEY_PREFIX + request.getAuthorizationId();
        AuthorizationResponse authorizationResponse = (AuthorizationResponse) redisTemplate.opsForValue().get(authorizationRequestKey);
        if (authorizationResponse == null) {
            throw new AuthException(AuthErrorCode.AUTHORIZATION_REQUEST_NOT_FOUND);
        }

        // 验证客户端
        RegisteredClient client = clientRepository.findByClientId(authorizationResponse.getClientId());
        if (client == null) {
            throw new AuthException(AuthErrorCode.CLIENT_NOT_FOUND);
        }

        // 验证授权范围
        if (!client.getScopes().containsAll(request.getApprovedScopes())) {
            throw new AuthException(AuthErrorCode.INVALID_APPROVED_SCOPES);
        }

        try {
            // 生成授权码
            String code = authorizationCodeService.createAuthorizationCode(
                authorizationResponse.getClientId(),
                authentication.getName(),
                authorizationResponse.getRedirectUri(),
                String.join(" ", request.getApprovedScopes()),
                request.getCodeChallenge(),
                request.getCodeChallengeMethod()
            );

            // 创建授权响应
            AuthorizationConsentResponse response = new AuthorizationConsentResponse();
            response.setAuthorizationCode(code);
            response.setState(authorizationResponse.getState());
            response.setRedirectUri(authorizationResponse.getRedirectUri());
            
            // 从Redis删除待处理的授权请求
            redisTemplate.delete(authorizationRequestKey);
            
            return response;

        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.AUTHORIZATION_CODE_GENERATE_ERROR);
        }
    }

    /**
     * 保存授权请求到Redis
     */
    public void savePendingAuthorization(String authorizationId, AuthorizationResponse response) {
        String redisKey = REDIS_KEY_PREFIX + authorizationId;
        redisTemplate.opsForValue().set(redisKey, response, AUTHORIZATION_REQUEST_TIMEOUT, TimeUnit.MINUTES);
    }

    /**
     * 生成授权码
     */
    private String generateAuthorizationCode() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 处理授权同意请求
     */
    @Transactional
    public String consent(ConsentRequest request) {
        try {
            // 验证客户端
            RegisteredClient registeredClient = clientRepository.findByClientId(request.getClientId());
            if (registeredClient == null) {
                throw new AuthException(AuthErrorCode.CLIENT_NOT_FOUND);
            }

            // 创建授权同意
            OAuth2AuthorizationConsent.Builder consentBuilder = OAuth2AuthorizationConsent.withId(
                request.getClientId(),
                request.getUserId()
            );
            
            // 添加授权范围
            for (String scope : request.getScopes()) {
                consentBuilder.scope(scope);
            }

            // 保存授权同意
            OAuth2AuthorizationConsent consent = consentBuilder.build();
            authorizationConsentService.save(consent);

            // 创建授权
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(request.getUserId())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);

            // 设置授权范围
            authorizationBuilder.authorizedScopes(request.getScopes());

            // 生成授权码
            String code = authorizationCodeService.createAuthorizationCode(
                request.getClientId(),
                request.getPrincipal(),
                request.getRedirectUri(),
                String.join(" ", request.getScopes()),
                null,
                null
            );

            // 保存授权信息
            OAuth2Authorization authorization = authorizationBuilder.build();
            authorizationService.save(authorization);

            return code;
        } catch (Exception e) {
            log.error("Failed to generate authorization code", e);
            throw new AuthException(AuthErrorCode.AUTHORIZATION_CODE_GENERATION_FAILED);
        }
    }
} 