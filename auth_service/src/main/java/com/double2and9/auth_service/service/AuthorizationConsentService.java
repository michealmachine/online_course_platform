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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthorizationConsentService {
    
    private final RegisteredClientRepository clientRepository;
    private final OAuth2AuthorizationService oauth2AuthorizationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthorizationCodeService authorizationCodeService;
    private final AuthorizationCodeRepository authorizationCodeRepository;
    
    private static final String REDIS_KEY_PREFIX = "oauth2:auth:request:";
    private static final long AUTHORIZATION_REQUEST_TIMEOUT = 10; // 10分钟过期

    @Transactional
    public AuthorizationConsentResponse consent(AuthorizationConsentRequest request, Authentication authentication) {
        // 验证用户是否已登录
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        // 从Redis获取待处理的授权请求
        String redisKey = REDIS_KEY_PREFIX + request.getAuthorizationId();
        AuthorizationResponse authorizationResponse = (AuthorizationResponse) redisTemplate.opsForValue().get(redisKey);
        if (authorizationResponse == null) {
            throw new AuthException(AuthErrorCode.AUTHORIZATION_REQUEST_NOT_FOUND);
        }

        // 验证授权范围
        RegisteredClient client = clientRepository.findByClientId(authorizationResponse.getClientId());
        if (!client.getScopes().containsAll(request.getApprovedScopes())) {
            throw new AuthException(AuthErrorCode.INVALID_APPROVED_SCOPES);
        }

        try {
            // 生成授权码
            String authorizationCode = authorizationCodeService.createAuthorizationCode(
                authorizationResponse.getClientId(),
                authentication.getName(),
                authorizationResponse.getRedirectUri(),
                String.join(" ", request.getApprovedScopes()),
                null,  // codeChallenge 暂时为空
                null   // codeChallengeMethod 暂时为空
            );

            // 创建授权响应
            AuthorizationConsentResponse response = new AuthorizationConsentResponse();
            response.setAuthorizationCode(authorizationCode);
            response.setState(authorizationResponse.getState());
            response.setRedirectUri(authorizationResponse.getRedirectUri());

            // 从Redis删除待处理的授权请求
            redisTemplate.delete(redisKey);

            // TODO: 保存授权码和相关信息到数据库

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

    public String consent(ConsentRequest request) {
        try {
            // 验证客户端
            RegisteredClient client = clientRepository.findByClientId(request.getClientId());
            if (client == null) {
                throw new AuthException(AuthErrorCode.INVALID_CLIENT_CREDENTIALS);
            }

            // 生成授权码前的验证
            if (request.getUserId() == null || request.getScope() == null) {
                throw new AuthException(AuthErrorCode.PARAMETER_VALIDATION_FAILED);
            }

            // 生成授权码
            AuthorizationCode authCode = new AuthorizationCode();
            authCode.setCode(generateAuthorizationCode());  // 使用已存在的方法
            authCode.setClientId(request.getClientId());
            authCode.setUserId(request.getUserId());
            authCode.setRedirectUri(request.getRedirectUri());
            authCode.setScope(request.getScope());
            authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
            authCode.setUsed(false);
            
            authorizationCodeRepository.save(authCode);
            
            return authCode.getCode();
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.AUTHORIZATION_CODE_GENERATE_ERROR);
        }
    }
} 