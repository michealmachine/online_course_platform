package com.double2and9.auth_service.utils;

import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.base.enums.AuthErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PKCEUtilsTest {

    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CODE_CHALLENGE_PLAIN = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CODE_CHALLENGE_S256 = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Test
    void verifyCodeChallenge_Success_Plain() {
        assertTrue(PKCEUtils.verifyCodeChallenge(
            CODE_VERIFIER,
            CODE_CHALLENGE_PLAIN,
            "plain"
        ));
    }

    @Test
    void verifyCodeChallenge_Success_S256() {
        assertTrue(PKCEUtils.verifyCodeChallenge(
            CODE_VERIFIER,
            CODE_CHALLENGE_S256,
            "S256"
        ));
    }

    @Test
    void verifyCodeChallenge_Failure_WrongVerifier() {
        assertFalse(PKCEUtils.verifyCodeChallenge(
            "wrong_verifier",
            CODE_CHALLENGE_S256,
            "S256"
        ));
    }

    @Test
    void verifyCodeChallenge_Failure_NullValues() {
        assertFalse(PKCEUtils.verifyCodeChallenge(null, CODE_CHALLENGE_S256, "S256"));
        assertFalse(PKCEUtils.verifyCodeChallenge(CODE_VERIFIER, null, "S256"));
    }

    @Test
    void computeCodeChallenge_Success_Plain() {
        assertEquals(
            CODE_CHALLENGE_PLAIN,
            PKCEUtils.computeCodeChallenge(CODE_VERIFIER, "plain")
        );
    }

    @Test
    void computeCodeChallenge_Success_S256() {
        assertEquals(
            CODE_CHALLENGE_S256,
            PKCEUtils.computeCodeChallenge(CODE_VERIFIER, "S256")
        );
    }

    @Test
    void computeCodeChallenge_InvalidMethod() {
        AuthException exception = assertThrows(
            AuthException.class,
            () -> PKCEUtils.computeCodeChallenge(CODE_VERIFIER, "invalid")
        );
        assertEquals(AuthErrorCode.INVALID_CODE_VERIFIER, exception.getErrorCode());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCodeVerifiers")
    void verifyCodeChallenge_InvalidCodeVerifier(String codeVerifier) {
        assertFalse(PKCEUtils.verifyCodeChallenge(
            codeVerifier,
            CODE_CHALLENGE_S256,
            "S256"
        ));
    }

    private static Stream<Arguments> provideInvalidCodeVerifiers() {
        return Stream.of(
            Arguments.of((String) null),
            Arguments.of(""),
            Arguments.of("   "),
            Arguments.of("too_short"),
            Arguments.of("invalid@characters")
        );
    }
} 