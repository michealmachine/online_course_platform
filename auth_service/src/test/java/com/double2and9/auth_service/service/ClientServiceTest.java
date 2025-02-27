package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.CreateClientRequest;
import com.double2and9.auth_service.dto.request.UpdateClientRequest;
import com.double2and9.auth_service.dto.response.ClientResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private CustomJdbcRegisteredClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientService clientService;

    private CreateClientRequest request;
    private RegisteredClient existingClient;

    @BeforeEach
    void setUp() {
        request = new CreateClientRequest();
        request.setClientId("test-client");
        request.setClientSecret("secret");
        request.setClientName("Test Client");
        request.setAuthenticationMethods(Set.of("client_secret_basic"));
        request.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
        request.setRedirectUris(Set.of("http://localhost:8080/callback"));
        request.setScopes(Set.of("read", "write"));

        // 创建一个完整的 RegisteredClient 用于测试
        existingClient = RegisteredClient.withId("1")
                .clientId("existing-client")
                .clientSecret("encoded-secret")
                .clientName("Existing Client")
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/callback")
                .scope("read")
                .scope("write")
                .build();
    }

    @Test
    void createClient_Success() {
        doReturn(null).when(clientRepository).findByClientId(request.getClientId());
        when(passwordEncoder.encode(any())).thenReturn("encoded-secret");
        
        // 使用 doAnswer 来模拟 void 方法
        doAnswer(invocation -> {
            RegisteredClient client = invocation.getArgument(0);
            return null; // void 方法返回 null
        }).when(clientRepository).save(any(RegisteredClient.class));
        
        var response = clientService.createClient(request);
        
        assertNotNull(response);
        assertEquals(request.getClientId(), response.getClientId());
        assertEquals(request.getClientName(), response.getClientName());
        verify(clientRepository).save(any(RegisteredClient.class));
    }

    @Test
    void createClient_ClientIdExists() {
        when(clientRepository.findByClientId(request.getClientId())).thenReturn(existingClient);
        
        assertThrows(AuthException.class, () -> clientService.createClient(request));
        verify(clientRepository, never()).save(any());
    }

    @Test
    void getClient_Success() {
        when(clientRepository.findByClientId("existing-client")).thenReturn(existingClient);
        
        var response = clientService.getClient("existing-client");
        
        assertNotNull(response);
        assertEquals("existing-client", response.getClientId());
        assertEquals("Existing Client", response.getClientName());
    }

    @Test
    void getClient_NotFound() {
        doReturn(null).when(clientRepository).findByClientId("non-existent");
        
        assertThrows(AuthException.class, () -> clientService.getClient("non-existent"));
    }

    @Test
    void updateClient_Success() {
        // 准备测试数据
        UpdateClientRequest request = new UpdateClientRequest();
        request.setClientName("Updated Client");
        request.setAuthenticationMethods(Set.of("client_secret_basic"));
        request.setAuthorizationGrantTypes(Set.of("authorization_code"));
        request.setScopes(Set.of("read"));

        when(clientRepository.findByClientId("test-client")).thenReturn(existingClient);
        // 使用 doAnswer 来模拟 void 方法
        doAnswer(invocation -> {
            RegisteredClient client = invocation.getArgument(0);
            return null;
        }).when(clientRepository).save(any(RegisteredClient.class));

        // 执行测试
        ClientResponse response = clientService.updateClient("test-client", request);

        // 验证结果
        assertNotNull(response);
        assertEquals(request.getClientName(), response.getClientName());
        verify(clientRepository).findByClientId("test-client");
        verify(clientRepository).save(any(RegisteredClient.class));
    }

    @Test
    void updateClient_NotFound() {
        UpdateClientRequest request = new UpdateClientRequest();
        request.setClientName("Updated Client");
        
        when(clientRepository.findByClientId("non-existent")).thenReturn(null);

        assertThrows(AuthException.class, () -> 
            clientService.updateClient("non-existent", request));
    }

    @Test
    void deleteClient_Success() {
        when(clientRepository.findByClientId("test-client")).thenReturn(existingClient);
        doNothing().when(clientRepository).deleteById(anyString());

        assertDoesNotThrow(() -> clientService.deleteClient("test-client"));
        verify(clientRepository).deleteById(existingClient.getId());
    }

    @Test
    void deleteClient_NotFound() {
        when(clientRepository.findByClientId("non-existent")).thenReturn(null);

        assertThrows(AuthException.class, () -> 
            clientService.deleteClient("non-existent"));
    }

    @Test
    void listClients_Success() {
        // 准备测试数据
        List<RegisteredClient> clients = List.of(existingClient);
        when(clientRepository.findAll()).thenReturn(clients);

        // 执行测试
        PageResult<ClientResponse> result = clientService.listClients(new PageParams(1L, 10L));

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getCounts());
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getPage());
        assertEquals(10L, result.getPageSize());
    }

    @Test
    void createClient_WithOidcScopes_Success() {
        // 准备测试数据
        CreateClientRequest request = new CreateClientRequest();
        request.setClientId("oidc-client");
        request.setClientSecret("secret");
        request.setClientName("OIDC Test Client");
        request.setAuthenticationMethods(Set.of("client_secret_basic"));
        request.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
        request.setRedirectUris(Set.of("http://localhost:8080/callback"));
        request.setScopes(Set.of("openid", "profile", "email", "phone", "address"));

        doReturn(null).when(clientRepository).findByClientId(request.getClientId());
        when(passwordEncoder.encode(any())).thenReturn("encoded-secret");
        
        // 使用 doAnswer 来模拟 void 方法
        doAnswer(invocation -> {
            RegisteredClient client = invocation.getArgument(0);
            return null;
        }).when(clientRepository).save(any(RegisteredClient.class));
        
        // 执行测试
        ClientResponse response = clientService.createClient(request);
        
        // 验证结果
        assertNotNull(response);
        assertEquals("oidc-client", response.getClientId());
        assertEquals("OIDC Test Client", response.getClientName());
        assertTrue(response.getScopes().containsAll(Set.of("openid", "profile", "email", "phone", "address")));
        assertEquals(5, response.getScopes().size());
    }

    @Test
    void updateClient_WithOidcScopes_Success() {
        // 准备测试数据
        UpdateClientRequest request = new UpdateClientRequest();
        request.setClientName("Test Client");
        request.setClientSecret("secret");
        request.setRedirectUris(Set.of("http://localhost:8080/callback"));
        request.setScopes(Set.of("openid", "profile"));
        request.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
        request.setAuthenticationMethods(Set.of("client_secret_basic"));
        
        RegisteredClient existingClient = RegisteredClient.withId("1")
            .clientId("test-client")
            .clientSecret("old-secret")
            .clientName("Old Client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/old-callback")
            .scope("old-scope")
            .build();

        when(clientRepository.findByClientId("test-client")).thenReturn(existingClient);
        doNothing().when(clientRepository).save(any(RegisteredClient.class));

        // 执行测试
        ClientResponse response = clientService.updateClient("test-client", request);

        // 验证结果
        assertNotNull(response);
        assertEquals("test-client", response.getClientId());
        assertEquals(request.getClientName(), response.getClientName());
        assertTrue(response.getScopes().containsAll(request.getScopes()));
        assertTrue(response.getRedirectUris().containsAll(request.getRedirectUris()));
        assertTrue(response.getAuthenticationMethods().contains("client_secret_basic"));
        assertTrue(response.getAuthorizationGrantTypes().containsAll(Set.of("authorization_code", "refresh_token")));

        // 验证save方法被调用
        verify(clientRepository).save(any(RegisteredClient.class));
    }
} 