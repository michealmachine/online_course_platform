package com.double2and9.auth_service.utils;

import lombok.extern.slf4j.Slf4j;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.SecureRandom;
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
    
    /**
     * 生成一个随机的 code_verifier
     * 按照 RFC 7636 规范，code_verifier 应该是一个长度在43到128之间的随机字符串
     * 包含字母、数字、"-"、"."、"_"和"~"
     */
    public static String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[64]; // 生成一个64字节的随机字符串
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(codeVerifier);
    }
    
    /**
     * 使用 S256 方法生成 code_challenge
     */
    public static String generateCodeChallenge(String codeVerifier) {
        return computeCodeChallenge(codeVerifier, "S256");
    }
} 