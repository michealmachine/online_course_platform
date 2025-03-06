package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.CreateUserRequest;
import com.double2and9.auth_service.dto.request.UpdateUserRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资源API的集成测试
 * 测试需要OAuth2授权的资源API
 */
public class ResourceApiIntegrationTest extends BaseOAuth2IntegrationTest {

    @Test
    @DisplayName("测试获取用户信息")
    @Transactional
    public void testGetUserInfo() throws Exception {
        // 测试获取用户详情API
        mockMvc.perform(get("/api/users/{id}", testUser.getId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    @DisplayName("测试创建用户")
    @Transactional
    public void testCreateUser() throws Exception {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("newuser");
        createUserRequest.setPassword("password123");
        createUserRequest.setEmail("newuser@example.com");
        createUserRequest.setRoles(new HashSet<>(Arrays.asList("ROLE_USER")));
        createUserRequest.setGivenName("New");
        createUserRequest.setFamilyName("User");
        createUserRequest.setPreferredUsername("newuser_preferred");
        
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.givenName").value("New"))
                .andExpect(jsonPath("$.familyName").value("User"))
                .andExpect(jsonPath("$.preferredUsername").value("newuser_preferred"));
    }

    @Test
    @DisplayName("测试更新用户信息")
    @Transactional
    public void testUpdateUser() throws Exception {
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setNickname("UpdatedNick");
        updateUserRequest.setGivenName("Updated");
        updateUserRequest.setFamilyName("User");
        updateUserRequest.setPreferredUsername("updated_user");
        
        mockMvc.perform(put("/api/users/{id}", testUser.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("UpdatedNick"))
                .andExpect(jsonPath("$.givenName").value("Updated"))
                .andExpect(jsonPath("$.familyName").value("User"))
                .andExpect(jsonPath("$.preferredUsername").value("updated_user"));
    }

    @Test
    @DisplayName("测试获取用户列表")
    @Transactional
    public void testGetUsersList() throws Exception {
        // 创建额外测试用户以确保分页工作正常
        createTestUser("extrauser1");
        createTestUser("extrauser2");
        
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("测试未授权访问用户API")
    @Transactional
    public void testUnauthorizedAccessUserApi() throws Exception {
        // 不提供访问令牌，应该返回401或者重定向到登录页
        MvcResult result = mockMvc.perform(get("/api/users")
                .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        
        int status = result.getResponse().getStatus();
        String redirectUrl = result.getResponse().getRedirectedUrl();
        
        // 允许未授权(401)或重定向到登录页面(302)两种行为
        assertTrue(
            status == HttpStatus.UNAUTHORIZED.value() || 
            (status == HttpStatus.FOUND.value() && 
             redirectUrl != null && 
             redirectUrl.contains("/auth/login")),
            "未授权访问应返回401或重定向到登录页面，但返回了: " + status
        );
    }

    @Test
    @DisplayName("测试获取客户端列表")
    @Transactional
    public void testGetClientsList() throws Exception {
        mockMvc.perform(get("/api/clients")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.counts", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("测试获取客户端详情")
    @Transactional
    public void testGetClientDetails() throws Exception {
        mockMvc.perform(get("/api/clients/{clientId}", firstPartyClient.getClientId())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(firstPartyClient.getClientId()))
                .andExpect(jsonPath("$.clientName").value(firstPartyClient.getClientName()));
    }

    @Test
    @DisplayName("测试创建客户端")
    @Transactional
    public void testCreateClient() throws Exception {
        // 构建创建客户端的请求
        String requestBody = "{"
                + "\"clientId\":\"test-client-" + System.currentTimeMillis() + "\","
                + "\"clientName\":\"Test Client\","
                + "\"clientSecret\":\"test-secret\","
                + "\"redirectUris\":[\"http://localhost:3000/callback\"],"
                + "\"authenticationMethods\":[\"client_secret_basic\"],"
                + "\"authorizationGrantTypes\":[\"authorization_code\",\"refresh_token\"],"
                + "\"scopes\":[\"read\",\"write\"]"
                + "}";
        
        MvcResult result = mockMvc.perform(post("/api/clients")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").exists())
                .andExpect(jsonPath("$.clientName").value("Test Client"))
                .andReturn();
        
        // 提取创建的客户端ID用于后续测试
        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        String createdClientId = responseJson.get("clientId").asText();
        
        // 验证客户端已创建并可以查询
        mockMvc.perform(get("/api/clients/{clientId}", createdClientId)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(createdClientId));
    }

    @Test
    @DisplayName("测试删除客户端")
    @Transactional
    public void testDeleteClient() throws Exception {
        // 首先创建一个测试客户端
        String clientId = "delete-test-client-" + System.currentTimeMillis();
        String requestBody = "{"
                + "\"clientId\":\"" + clientId + "\","
                + "\"clientName\":\"Delete Test Client\","
                + "\"clientSecret\":\"test-secret\","
                + "\"redirectUris\":[\"http://localhost:3000/callback\"],"
                + "\"grantTypes\":[\"authorization_code\",\"refresh_token\"],"
                + "\"scopes\":[\"read\",\"write\"],"
                + "\"authenticationMethods\":[\"client_secret_basic\"],"
                + "\"authorizationGrantTypes\":[\"authorization_code\",\"refresh_token\"]"
                + "}";
        
        // 创建客户端
        mockMvc.perform(post("/api/clients")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated());
        
        // 验证客户端存在
        mockMvc.perform(get("/api/clients/{clientId}", clientId)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        // 删除客户端
        mockMvc.perform(delete("/api/clients/{clientId}", clientId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
        
        // 验证客户端已删除
        mockMvc.perform(get("/api/clients/{clientId}", clientId)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("测试未授权访问客户端API")
    @Transactional
    public void testUnauthorizedAccessClientApi() throws Exception {
        // 不提供访问令牌，应该返回401
        mockMvc.perform(get("/api/clients")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
    
    // 辅助方法：创建测试用户
    private void createTestUser(String username) throws Exception {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(username);
        createUserRequest.setPassword("password123");
        createUserRequest.setEmail(username + "@example.com");
        createUserRequest.setRoles(new HashSet<>(Arrays.asList("ROLE_USER")));
        
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated());
    }
} 