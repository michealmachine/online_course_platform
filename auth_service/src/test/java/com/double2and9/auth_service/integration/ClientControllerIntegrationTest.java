package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.CreateClientRequest;
import com.double2and9.auth_service.dto.request.UpdateClientRequest;
import com.double2and9.auth_service.dto.response.ClientResponse;
import com.double2and9.base.enums.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientControllerIntegrationTest extends BaseOAuth2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegisteredClientRepository clientRepository;

    private CreateClientRequest request;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        request = new CreateClientRequest();
        request.setClientId("test-client");
        request.setClientSecret("secret");
        request.setClientName("Test Client");
        request.setAuthenticationMethods(Set.of("client_secret_basic"));
        request.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
        request.setRedirectUris(Set.of("http://localhost:8080/callback"));
        request.setScopes(Set.of("read", "write"));
    }

    @Test
    void createClient_Success() throws Exception {
        setupAdminUser();
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(request.getClientId()))
                .andExpect(jsonPath("$.clientName").value(request.getClientName()))
                .andExpect(jsonPath("$.authenticationMethods", hasSize(1)))
                .andExpect(jsonPath("$.authorizationGrantTypes", hasSize(2)))
                .andExpect(jsonPath("$.scopes", hasSize(2)));
    }

    @Test
    void createClient_DuplicateId() throws Exception {
        // 先创建一个客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 尝试创建相同ID的客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.CLIENT_ID_EXISTS.getCode()));
    }

    @Test
    void createClient_InsufficientPermissions() throws Exception {
        // 使用普通用户token
        setupUserWithToken();

        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createClient_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getClient_Success() throws Exception {
        // 先创建一个客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 获取创建的客户端
        mockMvc.perform(get("/api/clients/{clientId}", request.getClientId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(request.getClientId()))
                .andExpect(jsonPath("$.clientName").value(request.getClientName()));
    }

    @Test
    void getClient_NotFound() throws Exception {
        mockMvc.perform(get("/api/clients/{clientId}", "non-existent")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.CLIENT_NOT_FOUND.getCode()));
    }

    @Test
    void updateClient_Success() throws Exception {
        // 先创建一个客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 准备更新请求
        UpdateClientRequest updateRequest = new UpdateClientRequest();
        updateRequest.setClientName("Updated Client");
        updateRequest.setAuthenticationMethods(Set.of("client_secret_basic"));
        updateRequest.setAuthorizationGrantTypes(Set.of("authorization_code"));
        updateRequest.setScopes(Set.of("read"));

        // 执行更新
        mockMvc.perform(put("/api/clients/{clientId}", request.getClientId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value(updateRequest.getClientName()));
    }

    @Test
    void deleteClient_Success() throws Exception {
        // 先创建一个客户端
        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 删除客户端
        mockMvc.perform(delete("/api/clients/{clientId}", request.getClientId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // 验证客户端已被删除
        mockMvc.perform(get("/api/clients/{clientId}", request.getClientId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void listClients_Success() throws Exception {
        // 创建多个客户端
        for (int i = 0; i < 3; i++) {
            CreateClientRequest newRequest = new CreateClientRequest();
            newRequest.setClientId("test-client-" + i);
            newRequest.setClientSecret("secret");
            newRequest.setClientName("Test Client " + i);
            newRequest.setAuthenticationMethods(Set.of("client_secret_basic"));
            newRequest.setAuthorizationGrantTypes(Set.of("authorization_code"));
            newRequest.setRedirectUris(Set.of("http://localhost:8080/callback"));
            newRequest.setScopes(Set.of("read"));

            mockMvc.perform(post("/api/clients")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .content(objectMapper.writeValueAsString(newRequest)))
                    .andExpect(status().isCreated());
        }

        // 获取客户端列表
        mockMvc.perform(get("/api/clients")
                .header("Authorization", "Bearer " + accessToken)
                .param("pageNo", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.counts", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void createClient_WithOidcScopes_Success() throws Exception {
        CreateClientRequest request = new CreateClientRequest();
        request.setClientId("oidc-client");
        request.setClientSecret("secret");
        request.setClientName("OIDC Test Client");
        request.setAuthenticationMethods(Set.of("client_secret_basic"));
        request.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
        request.setRedirectUris(Set.of("http://localhost:8080/callback"));
        request.setScopes(Set.of("openid", "profile", "email", "phone", "address"));

        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value("oidc-client"))
                .andExpect(jsonPath("$.clientName").value("OIDC Test Client"))
                .andExpect(jsonPath("$.scopes", hasSize(5)))
                .andExpect(jsonPath("$.scopes", hasItems("openid", "profile", "email", "phone", "address")));
    }

    @Test
    void updateClient_WithOidcScopes_Success() throws Exception {
        // 先创建一个客户端
        CreateClientRequest createRequest = new CreateClientRequest();
        createRequest.setClientId("update-oidc-client");
        createRequest.setClientSecret("secret");
        createRequest.setClientName("Original OIDC Client");
        createRequest.setAuthenticationMethods(Set.of("client_secret_basic"));
        createRequest.setAuthorizationGrantTypes(Set.of("authorization_code"));
        createRequest.setRedirectUris(Set.of("http://localhost:8080/callback"));
        createRequest.setScopes(Set.of("openid", "profile"));

        mockMvc.perform(post("/api/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // 更新客户端
        UpdateClientRequest updateRequest = new UpdateClientRequest();
        updateRequest.setClientName("Updated OIDC Client");
        updateRequest.setAuthenticationMethods(Set.of("client_secret_basic"));
        updateRequest.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
        updateRequest.setScopes(Set.of("openid", "profile", "email", "phone"));

        mockMvc.perform(put("/api/clients/{clientId}", "update-oidc-client")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("Updated OIDC Client"))
                .andExpect(jsonPath("$.scopes", hasSize(4)))
                .andExpect(jsonPath("$.scopes", hasItems("openid", "profile", "email", "phone")));
    }
} 