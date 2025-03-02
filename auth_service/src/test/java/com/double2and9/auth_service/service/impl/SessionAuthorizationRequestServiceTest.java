package com.double2and9.auth_service.service.impl;

import com.double2and9.auth_service.dto.request.OAuth2AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionAuthorizationRequestServiceTest {

    @InjectMocks
    private SessionAuthorizationRequestService authorizationRequestService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;
    private OAuth2AuthorizationRequest authorizationRequest;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        session = new MockHttpSession();
        request.setSession(session);

        authorizationRequest = OAuth2AuthorizationRequest.builder()
                .clientId("test-client")
                .scope("openid profile")
                .state("test-state")
                .redirectUri("http://localhost:8080/callback")
                .responseType("code")
                .codeChallenge("test-challenge")
                .codeChallengeMethod("S256")
                .nonce("test-nonce")
                .continueAuthorization(true)
                .build();
    }

    @Test
    void saveAuthorizationRequest_ShouldStoreRequestInSession() {
        // When
        authorizationRequestService.saveAuthorizationRequest(authorizationRequest, request, response);

        // Then
        Object storedRequest = session.getAttribute("oauth2_auth_request");
        assertNotNull(storedRequest);
        assertEquals(authorizationRequest, storedRequest);
    }

    @Test
    void saveAuthorizationRequest_WithNull_ShouldRemoveFromSession() {
        // Given
        session.setAttribute("oauth2_auth_request", authorizationRequest);

        // When
        authorizationRequestService.saveAuthorizationRequest(null, request, response);

        // Then
        assertNull(session.getAttribute("oauth2_auth_request"));
    }

    @Test
    void getAuthorizationRequest_ShouldReturnStoredRequest() {
        // Given
        session.setAttribute("oauth2_auth_request", authorizationRequest);

        // When
        OAuth2AuthorizationRequest result = authorizationRequestService.getAuthorizationRequest(request, response);

        // Then
        assertNotNull(result);
        assertEquals(authorizationRequest, result);
    }

    @Test
    void getAuthorizationRequest_WithNoSession_ShouldReturnNull() {
        // Given
        request.setSession(null);

        // When
        OAuth2AuthorizationRequest result = authorizationRequestService.getAuthorizationRequest(request, response);

        // Then
        assertNull(result);
    }

    @Test
    void removeAuthorizationRequest_ShouldRemoveFromSession() {
        // Given
        session.setAttribute("oauth2_auth_request", authorizationRequest);

        // When
        authorizationRequestService.removeAuthorizationRequest(request, response);

        // Then
        assertNull(session.getAttribute("oauth2_auth_request"));
    }

    @Test
    void extractAuthorizationRequest_ShouldCreateRequestFromParameters() {
        // Given
        request.setParameter("client_id", "test-client");
        request.setParameter("scope", "openid profile");
        request.setParameter("state", "test-state");
        request.setParameter("redirect_uri", "http://localhost:8080/callback");
        request.setParameter("response_type", "code");
        request.setParameter("code_challenge", "test-challenge");
        request.setParameter("code_challenge_method", "S256");
        request.setParameter("nonce", "test-nonce");
        request.setParameter("continue_authorization", "true");

        // When
        OAuth2AuthorizationRequest result = authorizationRequestService.extractAuthorizationRequest(request);

        // Then
        assertNotNull(result);
        assertEquals("test-client", result.getClientId());
        assertEquals("openid profile", result.getScope());
        assertEquals("test-state", result.getState());
        assertEquals("http://localhost:8080/callback", result.getRedirectUri());
        assertEquals("code", result.getResponseType());
        assertEquals("test-challenge", result.getCodeChallenge());
        assertEquals("S256", result.getCodeChallengeMethod());
        assertEquals("test-nonce", result.getNonce());
        assertTrue(result.isContinueAuthorization());
    }

    @Test
    void extractAuthorizationRequest_WithoutClientId_ShouldReturnNull() {
        // Given request without client_id

        // When
        OAuth2AuthorizationRequest result = authorizationRequestService.extractAuthorizationRequest(request);

        // Then
        assertNull(result);
    }

    @Test
    void buildAuthorizationRequestUrl_ShouldCreateValidUrl() {
        // When
        String url = authorizationRequest.buildAuthorizationRequestUrl();

        // Then
        assertTrue(url.startsWith("/oauth2/authorize?"));
        assertTrue(url.contains("client_id=test-client"));
        assertTrue(url.contains("scope=openid profile"));
        assertTrue(url.contains("state=test-state"));
        assertTrue(url.contains("redirect_uri=http://localhost:8080/callback"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("code_challenge=test-challenge"));
        assertTrue(url.contains("code_challenge_method=S256"));
        assertTrue(url.contains("nonce=test-nonce"));
    }
} 