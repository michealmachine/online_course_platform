package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.AuthorizationRequest;
import com.double2and9.auth_service.dto.request.CreateClientRequest;
import com.double2and9.base.enums.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateClientRequest clientRequest;
    private AuthorizationRequest authRequest;

    @BeforeEach
    void setUp() {
        // 准备测试客户端
        clientRequest = new CreateClientRequest();
        clientRequest.setClientId("test-client");
        clientRequest.setClientSecret("secret");
        clientRequest.setClientName("Test Client");
        clientRequest.setAuthenticationMethods(Set.of("client_secret_basic"));
        clientRequest.setAuthorizationGrantTypes(Set.of("authorization_code"));
        clientRequest.setRedirectUris(Set.of("http://localhost:8080/callback"));
        clientRequest.setScopes(Set.of("read", "write"));

        // 准备授权请求
        authRequest = new AuthorizationRequest();
        authRequest.setResponseType("code");
        authRequest.setClientId("test-client");
        authRequest.setRedirectUri("http://localhost:8080/callback");
        authRequest.setScope("read write");
        authRequest.setState("xyz");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorize_Success() throws Exception {
        // 先创建客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 设置PKCE参数
        authRequest.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        authRequest.setCodeChallengeMethod("S256");

        // 测试授权请求
        mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(authRequest.getClientId()))
                .andExpect(jsonPath("$.clientName").value(clientRequest.getClientName()))
                .andExpect(jsonPath("$.requestedScopes", containsInAnyOrder("read", "write")))
                .andExpect(jsonPath("$.state").value(authRequest.getState()))
                .andExpect(jsonPath("$.authorizationId").isNotEmpty())
                .andExpect(jsonPath("$.codeChallenge").value(authRequest.getCodeChallenge()))
                .andExpect(jsonPath("$.codeChallengeMethod").value(authRequest.getCodeChallengeMethod()));
    }

    @Test
    void authorize_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user")
    void authorize_ClientNotFound() throws Exception {
        mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.CLIENT_NOT_FOUND.getCode()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorize_InvalidRedirectUri() throws Exception {
        // 先创建客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 使用无效的重定向URI
        authRequest.setRedirectUri("http://evil.com");
        mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.CLIENT_REDIRECT_URI_INVALID.getCode()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorize_WithoutPKCE_Fails() throws Exception {
        // 先创建客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 不设置PKCE参数
        authRequest.setCodeChallenge(null);
        authRequest.setCodeChallengeMethod(null);

        // 测试授权请求
        mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())  // 改为400错误
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PKCE_REQUIRED.getCode()));
    }
    
    // 新增测试方法：使用GET方法进行授权请求
    @Test
    @WithMockUser(roles = "ADMIN")
    void authorizeGet_Success() throws Exception {
        // 先创建客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 测试GET方法授权请求
        mockMvc.perform(get("/api/oauth2/authorize")
                .param("response_type", authRequest.getResponseType())
                .param("client_id", authRequest.getClientId())
                .param("redirect_uri", authRequest.getRedirectUri())
                .param("scope", authRequest.getScope())
                .param("state", authRequest.getState())
                .param("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                .param("code_challenge_method", "S256"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(authRequest.getClientId()))
                .andExpect(jsonPath("$.clientName").value(clientRequest.getClientName()))
                .andExpect(jsonPath("$.requestedScopes", containsInAnyOrder("read", "write")))
                .andExpect(jsonPath("$.state").value(authRequest.getState()))
                .andExpect(jsonPath("$.authorizationId").isNotEmpty())
                .andExpect(jsonPath("$.codeChallenge").value("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"))
                .andExpect(jsonPath("$.codeChallengeMethod").value("S256"));
    }
    
    @Test
    void authorizeGet_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/oauth2/authorize")
                .param("response_type", authRequest.getResponseType())
                .param("client_id", authRequest.getClientId())
                .param("redirect_uri", authRequest.getRedirectUri())
                .param("scope", authRequest.getScope())
                .param("state", authRequest.getState()))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(username = "user")
    void authorizeGet_ClientNotFound() throws Exception {
        mockMvc.perform(get("/api/oauth2/authorize")
                .param("response_type", authRequest.getResponseType())
                .param("client_id", authRequest.getClientId())
                .param("redirect_uri", authRequest.getRedirectUri())
                .param("scope", authRequest.getScope())
                .param("state", authRequest.getState()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.CLIENT_NOT_FOUND.getCode()));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void authorizeGet_InvalidRedirectUri() throws Exception {
        // 先创建客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 使用无效的重定向URI
        mockMvc.perform(get("/api/oauth2/authorize")
                .param("response_type", authRequest.getResponseType())
                .param("client_id", authRequest.getClientId())
                .param("redirect_uri", "http://evil.com")
                .param("scope", authRequest.getScope())
                .param("state", authRequest.getState()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.CLIENT_REDIRECT_URI_INVALID.getCode()));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void authorizeGet_WithoutPKCE_Fails() throws Exception {
        // 先创建客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 测试不带PKCE参数的请求
        mockMvc.perform(get("/api/oauth2/authorize")
                .param("response_type", authRequest.getResponseType())
                .param("client_id", authRequest.getClientId())
                .param("redirect_uri", authRequest.getRedirectUri())
                .param("scope", authRequest.getScope())
                .param("state", authRequest.getState()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PKCE_REQUIRED.getCode()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorize_WithOpenIdScope_Success() throws Exception {
        // 先创建客户端
        clientRequest.setScopes(Set.of("openid", "profile", "email"));
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 设置PKCE参数和openid scope
        authRequest.setScope("openid profile email");
        authRequest.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        authRequest.setCodeChallengeMethod("S256");

        // 测试授权请求
        mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(authRequest.getClientId()))
                .andExpect(jsonPath("$.clientName").value(clientRequest.getClientName()))
                .andExpect(jsonPath("$.requestedScopes", containsInAnyOrder("openid", "profile", "email")))
                .andExpect(jsonPath("$.state").value(authRequest.getState()))
                .andExpect(jsonPath("$.authorizationId").isNotEmpty())
                .andExpect(jsonPath("$.codeChallenge").value(authRequest.getCodeChallenge()))
                .andExpect(jsonPath("$.codeChallengeMethod").value(authRequest.getCodeChallengeMethod()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authorize_WithInvalidOpenIdScope_Fails() throws Exception {
        // 先创建客户端，但不包含openid scope
        clientRequest.setScopes(Set.of("profile", "email"));
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 尝试请求openid scope
        authRequest.setScope("openid profile email");
        authRequest.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        authRequest.setCodeChallengeMethod("S256");

        mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.CLIENT_SCOPE_INVALID.getCode()));
    }
} 