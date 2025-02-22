package com.double2and9.auth_service.service;

import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
import com.double2and9.base.enums.AuthErrorCode;
import com.double2and9.auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthorizationCodeService {
    
    private final AuthorizationCodeRepository authorizationCodeRepository;
    private static final int CODE_EXPIRES_IN = 10;  // 10分钟过期

    @Transactional
    public String createAuthorizationCode(String clientId, String userId, String redirectUri, 
            String scope, String codeChallenge, String codeChallengeMethod) {
        // 生成授权码
        String code = generateCode();

        // 创建授权码记录
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode(code);
        authCode.setClientId(clientId);
        authCode.setUserId(userId);
        authCode.setRedirectUri(redirectUri);
        authCode.setScope(scope);
        authCode.setCodeChallenge(codeChallenge);
        authCode.setCodeChallengeMethod(codeChallengeMethod);
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRES_IN));
        authCode.setUsed(false);

        authorizationCodeRepository.save(authCode);
        return code;
    }

    @Transactional
    public AuthorizationCode validateAndConsume(String code, String clientId, String redirectUri) {
        AuthorizationCode authCode = authorizationCodeRepository.findByCode(code)
            .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_AUTHORIZATION_CODE));

        // 验证授权码
        if (authCode.isUsed()) {
            throw new AuthException(AuthErrorCode.AUTHORIZATION_CODE_USED);
        }
        if (LocalDateTime.now().isAfter(authCode.getExpiresAt())) {
            throw new AuthException(AuthErrorCode.AUTHORIZATION_CODE_EXPIRED);
        }
        if (!authCode.getClientId().equals(clientId)) {
            throw new AuthException(AuthErrorCode.INVALID_CLIENT);
        }
        if (!authCode.getRedirectUri().equals(redirectUri)) {
            throw new AuthException(AuthErrorCode.CLIENT_REDIRECT_URI_INVALID);
        }

        // 标记授权码为已使用
        authCode.setUsed(true);
        authorizationCodeRepository.save(authCode);

        return authCode;
    }

    private String generateCode() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
} 