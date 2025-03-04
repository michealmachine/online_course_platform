package com.double2and9.auth_service.service;

import com.double2and9.auth_service.entity.AuthorizationCode;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.AuthorizationCodeRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.base.enums.AuthErrorCode;
import com.double2and9.auth_service.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationCodeService {
    
    private final AuthorizationCodeRepository authorizationCodeRepository;
    private final UserRepository userRepository;
    private static final int CODE_EXPIRES_IN = 10;  // 10分钟过期

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public String createAuthorizationCode(String clientId, String username, String redirectUri, 
            String scope, String codeChallenge, String codeChallengeMethod) {
        log.info("开始创建授权码，用户名: {}, 客户端ID: {}", username, clientId);
        
        // 通过用户名查找用户
        log.debug("正在查询用户: {}", username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("用户不存在: {}", username);
                return new AuthException(AuthErrorCode.USER_NOT_FOUND);
            });
        log.info("找到用户，ID: {}, 用户名: {}", user.getId(), user.getUsername());

        // 生成授权码
        String code = generateCode();
        log.debug("生成授权码: {}", code);

        // 创建授权码记录
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode(code);
        authCode.setClientId(clientId);
        authCode.setUserId(user.getId().toString());  // 存储用户 ID 而不是用户名
        authCode.setRedirectUri(redirectUri);
        authCode.setScope(scope);
        authCode.setCodeChallenge(codeChallenge);
        authCode.setCodeChallengeMethod(codeChallengeMethod);
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRES_IN));
        authCode.setUsed(false);

        log.debug("保存授权码记录: {}", authCode);
        authorizationCodeRepository.save(authCode);
        log.info("授权码创建成功，用户: {}, 客户端: {}", username, clientId);
        
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

    /**
     * 获取最新的有效授权码
     * 用于测试目的，获取指定用户和客户端的最新有效授权码
     * 
     * @param username 用户名
     * @param clientId 客户端ID
     * @return 最新的有效授权码，如果不存在则返回null
     */
    @Transactional(readOnly = true)
    public String getLatestAuthorizationCode(String username, String clientId) {
        log.info("获取最新的有效授权码，用户名: {}, 客户端ID: {}", username, clientId);
        
        // 通过用户名查找用户
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("尝试获取授权码的用户不存在: {}", username);
            return null;
        }
        
        // 查询该用户最新的未使用的有效授权码
        LocalDateTime now = LocalDateTime.now();
        return authorizationCodeRepository.findFirstByUserIdAndClientIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                String.valueOf(user.getId()), clientId, now)
            .map(AuthorizationCode::getCode)
            .orElseGet(() -> {
                // 如果没有找到有效的授权码，为了测试目的，创建一个新的
                log.info("没有找到有效的授权码，为测试目的创建一个新的");
                String newCode = createAuthorizationCode(
                    clientId, 
                    username, 
                    "http://localhost:3000/callback", 
                    "openid profile email",
                    null,  // 对于测试，不需要codeChallenge
                    null   // 对于测试，不需要codeChallengeMethod
                );
                return newCode;
            });
    }
} 