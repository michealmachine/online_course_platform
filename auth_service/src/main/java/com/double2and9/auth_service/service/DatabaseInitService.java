package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.CreateClientRequest;
import com.double2and9.auth_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseInitService {
    
    private final RoleRepository roleRepository;
    private final ClientService clientService;

    @Value("${app.client.web.secret:web-client-secret}")
    private String webClientSecret;

    @Value("${app.client.mobile.secret:mobile-client-secret}")
    private String mobileClientSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Checking database initialization status...");
        
        // 验证基础角色是否存在
        roleRepository.findByName("ROLE_USER").ifPresentOrElse(
            role -> log.info("Default role ROLE_USER exists"),
            () -> log.error("Default role ROLE_USER not found!")
        );

        // 初始化内部客户端
        initInternalClients();
    }

    /**
     * 初始化内部客户端（第一方应用）
     */
    private void initInternalClients() {
        // 初始化Web前端客户端
        initWebClient();
        
        // 初始化移动端客户端
        initMobileClient();
    }

    /**
     * 初始化Web前端客户端
     */
    private void initWebClient() {
        String clientId = "web-client";
        
        // 检查客户端是否已存在
        clientService.findByClientId(clientId).ifPresentOrElse(
            client -> log.info("Web client already exists: {}", clientId),
            () -> {
                log.info("Creating Web client: {}", clientId);
                
                CreateClientRequest request = new CreateClientRequest();
                request.setClientId(clientId);
                request.setClientName("Web前端应用");
                request.setClientSecret(webClientSecret);
                
                // 设置认证方法
                Set<String> authMethods = new HashSet<>();
                authMethods.add("client_secret_basic");
                request.setAuthenticationMethods(authMethods);
                
                // 设置授权类型
                Set<String> grantTypes = new HashSet<>();
                grantTypes.add("authorization_code");
                grantTypes.add("refresh_token");
                request.setAuthorizationGrantTypes(grantTypes);
                
                // 设置重定向URI
                Set<String> redirectUris = new HashSet<>();
                redirectUris.add("http://localhost:3000/callback");
                redirectUris.add("http://localhost:3000/silent-renew.html");
                request.setRedirectUris(redirectUris);
                
                // 设置作用域
                Set<String> scopes = new HashSet<>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("email");
                scopes.add("read");
                scopes.add("write");
                request.setScopes(scopes);
                
                // 设置为内部客户端，启用自动授权
                request.setIsInternal(true);
                request.setAutoApprove(true);
                
                try {
                    clientService.createClient(request);
                    log.info("Web client created successfully");
                } catch (Exception e) {
                    log.error("Failed to create Web client", e);
                }
            }
        );
    }

    /**
     * 初始化移动端客户端
     */
    private void initMobileClient() {
        String clientId = "mobile-client";
        
        // 检查客户端是否已存在
        clientService.findByClientId(clientId).ifPresentOrElse(
            client -> log.info("Mobile client already exists: {}", clientId),
            () -> {
                log.info("Creating Mobile client: {}", clientId);
                
                CreateClientRequest request = new CreateClientRequest();
                request.setClientId(clientId);
                request.setClientName("移动端应用");
                request.setClientSecret(mobileClientSecret);
                
                // 设置认证方法
                Set<String> authMethods = new HashSet<>();
                authMethods.add("client_secret_basic");
                request.setAuthenticationMethods(authMethods);
                
                // 设置授权类型
                Set<String> grantTypes = new HashSet<>();
                grantTypes.add("authorization_code");
                grantTypes.add("refresh_token");
                request.setAuthorizationGrantTypes(grantTypes);
                
                // 设置重定向URI
                Set<String> redirectUris = new HashSet<>();
                redirectUris.add("com.double2and9.app:/callback");
                redirectUris.add("https://oauth.double2and9.com/mobile/callback");
                request.setRedirectUris(redirectUris);
                
                // 设置作用域
                Set<String> scopes = new HashSet<>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("email");
                scopes.add("read");
                scopes.add("write");
                scopes.add("mobile");
                request.setScopes(scopes);
                
                // 设置为内部客户端，启用自动授权
                request.setIsInternal(true);
                request.setAutoApprove(true);
                
                try {
                    clientService.createClient(request);
                    log.info("Mobile client created successfully");
                } catch (Exception e) {
                    log.error("Failed to create Mobile client", e);
                }
            }
        );
    }
} 