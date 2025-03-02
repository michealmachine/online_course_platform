package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.CreateClientRequest;
import com.double2and9.auth_service.dto.request.UpdateClientRequest;
import com.double2and9.auth_service.dto.response.ClientResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.base.enums.AuthErrorCode;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {
    
    private final CustomJdbcRegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        // 检查客户端ID是否已存在
        if (clientRepository.findByClientId(request.getClientId()) != null) {
            throw new AuthException(AuthErrorCode.CLIENT_ID_EXISTS);
        }

        // 构建ClientSettings，考虑内部客户端自动授权
        ClientSettings clientSettings = ClientSettings.builder()
                .requireAuthorizationConsent(!Boolean.TRUE.equals(request.getAutoApprove()))
                .requireProofKey(true)
                .build();

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.getClientId())
                .clientSecret(passwordEncoder.encode(request.getClientSecret()))
                .clientName(request.getClientName())
                .clientIdIssuedAt(Instant.now())
                .clientAuthenticationMethods(methods -> 
                    request.getAuthenticationMethods().forEach(method -> 
                        methods.add(resolveClientAuthenticationMethod(method))))
                .authorizationGrantTypes(types -> 
                    request.getAuthorizationGrantTypes().forEach(type -> 
                        types.add(resolveAuthorizationGrantType(type))))
                .scopes(scopes -> 
                    request.getScopes().forEach(scopes::add))
                .clientSettings(clientSettings)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .build());

        // 处理 redirectUris 为 null 的情况
        if (request.getRedirectUris() != null && !request.getRedirectUris().isEmpty()) {
            builder.redirectUris(uris -> request.getRedirectUris().forEach(uris::add));
        }

        RegisteredClient client = builder.build();
        
        // 设置内部客户端标识和自动授权标识
        clientRepository.save(client, request.getIsInternal(), request.getAutoApprove());
        return toClientResponse(client, request.getIsInternal(), request.getAutoApprove());
    }

    public ClientResponse getClient(String clientId) {
        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new AuthException(AuthErrorCode.CLIENT_NOT_FOUND);
        }
        Boolean isInternal = clientRepository.isInternalClient(clientId);
        Boolean autoApprove = clientRepository.isAutoApproveClient(clientId);
        return toClientResponse(client, isInternal, autoApprove);
    }

    @Transactional
    public ClientResponse updateClient(String clientId, UpdateClientRequest request) {
        RegisteredClient existingClient = clientRepository.findByClientId(clientId);
        if (existingClient == null) {
            throw new AuthException(AuthErrorCode.CLIENT_NOT_FOUND);
        }

        // 构建ClientSettings，考虑内部客户端自动授权
        ClientSettings clientSettings = ClientSettings.builder()
                .requireAuthorizationConsent(!Boolean.TRUE.equals(request.getAutoApprove()))
                .requireProofKey(true)
                .build();

        RegisteredClient.Builder builder = RegisteredClient.from(existingClient)
                .clientName(request.getClientName())
                .clientAuthenticationMethods(methods -> {
                    methods.clear();
                    request.getAuthenticationMethods().forEach(method ->
                            methods.add(resolveClientAuthenticationMethod(method)));
                })
                .authorizationGrantTypes(types -> {
                    types.clear();
                    request.getAuthorizationGrantTypes().forEach(type ->
                            types.add(resolveAuthorizationGrantType(type)));
                })
                .scopes(scopes -> {
                    scopes.clear();
                    request.getScopes().forEach(scopes::add);
                })
                .clientSettings(clientSettings);

        // 如果提供了新的密钥，则更新密钥
        if (request.getClientSecret() != null && !request.getClientSecret().isEmpty()) {
            builder.clientSecret(passwordEncoder.encode(request.getClientSecret()));
        }

        // 更新重定向URI
        if (request.getRedirectUris() != null) {
            builder.redirectUris(uris -> {
                uris.clear();
                request.getRedirectUris().forEach(uris::add);
            });
        }

        RegisteredClient updatedClient = builder.build();
        clientRepository.save(updatedClient, request.getIsInternal(), request.getAutoApprove());
        return toClientResponse(updatedClient, request.getIsInternal(), request.getAutoApprove());
    }

    @Transactional
    public void deleteClient(String clientId) {
        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new AuthException(AuthErrorCode.CLIENT_NOT_FOUND);
        }
        // 这里可能需要检查客户端是否有关联的授权记录
        clientRepository.deleteById(client.getId());
    }

    public PageResult<ClientResponse> listClients(PageParams pageParams) {
        // 注意：JdbcRegisteredClientRepository 可能需要扩展以支持分页
        // 这里是一个简化的实现
        List<RegisteredClient> clients = clientRepository.findAll();
        int start = (pageParams.getPageNo().intValue() - 1) * pageParams.getPageSize().intValue();
        int end = Math.min((start + pageParams.getPageSize().intValue()), clients.size());
        
        List<ClientResponse> clientResponses = clients.subList(start, end)
                .stream()
                .map(client -> {
                    Boolean isInternal = clientRepository.isInternalClient(client.getClientId());
                    Boolean autoApprove = clientRepository.isAutoApproveClient(client.getClientId());
                    return toClientResponse(client, isInternal, autoApprove);
                })
                .collect(Collectors.toList());
                
        return new PageResult<>(
            clientResponses,
            clients.size(),
            pageParams.getPageNo(),
            pageParams.getPageSize()
        );
    }

    /**
     * 检查客户端是否为内部客户端
     */
    public boolean isInternalClient(String clientId) {
        return clientRepository.isInternalClient(clientId);
    }

    /**
     * 检查客户端是否自动授权
     */
    public boolean isAutoApproveClient(String clientId) {
        return clientRepository.isAutoApproveClient(clientId);
    }

    /**
     * 转换认证方法
     */
    private ClientAuthenticationMethod resolveClientAuthenticationMethod(String method) {
        return switch (method.toLowerCase()) {
            case "client_secret_basic" -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            case "client_secret_post" -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
            case "private_key_jwt" -> ClientAuthenticationMethod.PRIVATE_KEY_JWT;
            case "none" -> ClientAuthenticationMethod.NONE;
            default -> throw new AuthException(AuthErrorCode.CLIENT_AUTH_METHOD_INVALID);
        };
    }

    /**
     * 转换授权类型
     */
    private AuthorizationGrantType resolveAuthorizationGrantType(String type) {
        return switch (type.toLowerCase()) {
            case "authorization_code" -> AuthorizationGrantType.AUTHORIZATION_CODE;
            case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
            case "client_credentials" -> AuthorizationGrantType.CLIENT_CREDENTIALS;
            default -> throw new AuthException(AuthErrorCode.CLIENT_GRANT_TYPE_INVALID);
        };
    }

    /**
     * 转换为响应对象
     */
    private ClientResponse toClientResponse(RegisteredClient client, Boolean isInternal, Boolean autoApprove) {
        ClientResponse response = new ClientResponse();
        response.setId(client.getId());
        response.setClientId(client.getClientId());
        response.setClientIdIssuedAt(client.getClientIdIssuedAt());
        response.setClientName(client.getClientName());
        response.setAuthenticationMethods(client.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue)
                .collect(Collectors.toSet()));
        response.setAuthorizationGrantTypes(client.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .collect(Collectors.toSet()));
        response.setRedirectUris(new HashSet<>(client.getRedirectUris()));
        response.setScopes(new HashSet<>(client.getScopes()));
        response.setIsInternal(isInternal);
        response.setAutoApprove(autoApprove);
        return response;
    }
} 