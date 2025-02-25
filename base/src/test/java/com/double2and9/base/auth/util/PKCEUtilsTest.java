package com.double2and9.base.auth.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PKCEUtils工具类的单元测试
 */
class PKCEUtilsTest {

    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CODE_CHALLENGE_PLAIN = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CODE_CHALLENGE_S256 = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Test
    void shouldVerifyPlainCodeChallenge() {
        assertTrue(PKCEUtils.verifyCodeChallenge(
            CODE_VERIFIER,
            CODE_CHALLENGE_PLAIN,
            "plain"
        ));
    }

    @Test
    void shouldVerifyS256CodeChallenge() {
        assertTrue(PKCEUtils.verifyCodeChallenge(
            CODE_VERIFIER,
            CODE_CHALLENGE_S256,
            "S256"
        ));
    }

    @Test
    void shouldReturnFalseForWrongVerifier() {
        assertFalse(PKCEUtils.verifyCodeChallenge(
            "wrong_verifier",
            CODE_CHALLENGE_S256,
            "S256"
        ));
    }

    @Test
    void shouldReturnFalseForNullValues() {
        assertFalse(PKCEUtils.verifyCodeChallenge(null, CODE_CHALLENGE_S256, "S256"));
        assertFalse(PKCEUtils.verifyCodeChallenge(CODE_VERIFIER, null, "S256"));
    }

    @Test
    void shouldComputePlainCodeChallenge() {
        assertEquals(
            CODE_CHALLENGE_PLAIN,
            PKCEUtils.computeCodeChallenge(CODE_VERIFIER, "plain")
        );
    }

    @Test
    void shouldComputeS256CodeChallenge() {
        assertEquals(
            CODE_CHALLENGE_S256,
            PKCEUtils.computeCodeChallenge(CODE_VERIFIER, "S256")
        );
    }

    @Test
    void shouldThrowExceptionForInvalidMethod() {
        assertThrows(
            IllegalArgumentException.class,
            () -> PKCEUtils.computeCodeChallenge(CODE_VERIFIER, "invalid")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "too_short"})
    void shouldReturnFalseForInvalidCodeVerifiers(String codeVerifier) {
        assertFalse(PKCEUtils.verifyCodeChallenge(
            codeVerifier,
            CODE_CHALLENGE_S256,
            "S256"
        ));
    }
} 