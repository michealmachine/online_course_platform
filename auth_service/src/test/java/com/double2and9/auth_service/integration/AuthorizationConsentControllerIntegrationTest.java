package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.AuthorizationConsentRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationConsentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateClientRequest clientRequest;
    private AuthorizationRequest authRequest;
    private AuthorizationConsentRequest consentRequest;
    private String authorizationId;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        clientRequest = new CreateClientRequest();
        clientRequest.setClientId("test-client");
        clientRequest.setClientSecret("secret");
        clientRequest.setClientName("Test Client");
        clientRequest.setAuthenticationMethods(Set.of("client_secret_basic"));
        clientRequest.setAuthorizationGrantTypes(Set.of("authorization_code"));
        clientRequest.setRedirectUris(Set.of("http://localhost:8080/callback"));
        clientRequest.setScopes(Set.of("read", "write"));

        authRequest = new AuthorizationRequest();
        authRequest.setResponseType("code");
        authRequest.setClientId("test-client");
        authRequest.setRedirectUri("http://localhost:8080/callback");
        authRequest.setScope("read write");
        authRequest.setState("xyz");
        authRequest.setCodeChallenge("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        authRequest.setCodeChallengeMethod("S256");

        consentRequest = new AuthorizationConsentRequest();
        consentRequest.setApprovedScopes(Set.of("read", "write"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void consent_Success() throws Exception {
        // 先创建客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isCreated());

        // 需要先进行授权请求
        mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk());  // 这里失败了，因为缺少PKCE参数

        // 获取授权ID并设置重定向URI
        String response = mockMvc.perform(post("/api/oauth2/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 获取授权ID并设置重定向URI
        var responseObj = objectMapper.readTree(response);
        authorizationId = responseObj.get("authorizationId").asText();
        consentRequest.setAuthorizationId(authorizationId);

        // 确认授权
        mockMvc.perform(post("/api/oauth2/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationCode").exists())
                .andExpect(jsonPath("$.state").value("xyz"))
                .andExpect(jsonPath("$.redirectUri").value("http://localhost:8080/callback"));
    }

    @Test
    void consent_Unauthorized() throws Exception {
        consentRequest.setAuthorizationId("test-auth-id");
        mockMvc.perform(post("/api/oauth2/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consentRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user")
    void consent_RequestNotFound() throws Exception {
        consentRequest.setAuthorizationId("non-existent");
        mockMvc.perform(post("/api/oauth2/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consentRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.AUTHORIZATION_REQUEST_NOT_FOUND.getCode()));
    }
} 