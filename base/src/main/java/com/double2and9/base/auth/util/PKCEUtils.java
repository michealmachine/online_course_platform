package com.double2and9.base.auth.util;

import lombok.extern.slf4j.Slf4j;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * PKCE (Proof Key for Code Exchange) 工具类
 * 用于OAuth2授权码流程中的PKCE安全增强
 */
@Slf4j
public class PKCEUtils {
    
    /**
     * 验证 code_verifier 是否匹配 code_challenge
     *
     * @param codeVerifier 客户端提供的验证码
     * @param codeChallenge 之前存储的挑战码
     * @param codeChallengeMethod 挑战码生成方法 (plain或S256)
     * @return 是否匹配
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
     *
     * @param codeVerifier 客户端生成的验证码
     * @param method 挑战码生成方法 (plain或S256)
     * @return 计算的挑战码
     * @throws IllegalArgumentException 如果方法不支持
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
                throw new RuntimeException("SHA-256 algorithm not found", e);
            }
        }
        
        throw new IllegalArgumentException("Invalid code challenge method: " + method);
    }
} 