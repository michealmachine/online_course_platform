package com.double2and9.auth_service.service;

import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OidcAuthorizationService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String NONCE_KEY_PREFIX = "oidc:nonce:";
    private static final long NONCE_EXPIRATION = 10; // 10分钟过期
    
    /**
     * 验证授权请求中的 nonce 参数
     * @param clientId 客户端ID
     * @param nonce nonce参数（可选）
     * @param scope 请求的scope
     */
    public void validateAuthorizationRequest(String clientId, String nonce, String scope) {
        // 验证scope中是否包含openid
        if (!scope.contains("openid")) {
            throw new AuthException(AuthErrorCode.INVALID_SCOPE);
        }
        
        // 如果提供了nonce，则存储它
        if (StringUtils.hasText(nonce)) {
            String key = NONCE_KEY_PREFIX + clientId + ":" + nonce;
            redisTemplate.opsForValue().set(key, nonce, NONCE_EXPIRATION, TimeUnit.MINUTES);
        }
    }
    
    /**
     * 验证并消费nonce
     * @param clientId 客户端ID
     * @param nonce nonce参数
     * @return 是否验证通过
     */
    public boolean validateAndConsumeNonce(String clientId, String nonce) {
        if (!StringUtils.hasText(nonce)) {
            return true;  // 如果没有提供nonce，则认为验证通过
        }
        
        String key = NONCE_KEY_PREFIX + clientId + ":" + nonce;
        String storedNonce = redisTemplate.opsForValue().get(key);
        if (storedNonce != null) {
            redisTemplate.<String>delete(key);
            return true;
        }
        return false;
    }
} 