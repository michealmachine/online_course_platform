package com.double2and9.auth_service.utils;

import lombok.extern.slf4j.Slf4j;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.base.enums.AuthErrorCode;

@Slf4j
public class PKCEUtils {
    
    /**
     * 验证 code_verifier 是否匹配 code_challenge
     */
    public static boolean verifyCodeChallenge(String codeVerifier, String codeChallenge, String codeChallengeMethod) {
        if (codeVerifier == null || codeChallenge == null) {
            return false;
        }

        String computedChallenge = computeCodeChallenge(codeVerifier, codeChallengeMethod);
        return codeChallenge.equals(computedChallenge);
    }

    /**
     * 根据 code_verifier 和方法计算 code_challenge
     */
    public static String computeCodeChallenge(String codeVerifier, String method) {
        if ("plain".equals(method)) {
            return codeVerifier;
        }
        
        if ("S256".equals(method)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                log.error("SHA-256 algorithm not found", e);
                throw new AuthException(AuthErrorCode.SYSTEM_ERROR);
            }
        }
        
        throw new AuthException(AuthErrorCode.INVALID_CODE_VERIFIER);
    }
} 