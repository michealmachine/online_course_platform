package com.double2and9.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;

/**
 * OAuth2 授权服务器配置
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    /**
     * 配置授权服务器的安全过滤链
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        // 自定义授权服务器配置
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            // 自定义授权确认页面处理
            .authorizationEndpoint(authorizationEndpoint ->
                authorizationEndpoint.consentPage("/oauth2/consent"))
            // 添加OIDC支持
            .oidc(Customizer.withDefaults());
        
        // 使用自定义登录页面
        http
            // 所有未认证的请求都重定向到登录页
            .exceptionHandling((exceptions) -> exceptions
                .authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/auth/login")))
            // 使用表单登录
            .formLogin(formLogin -> formLogin
                .loginPage("/auth/login")
                .permitAll());
            
        return http.build();
    }

    /**
     * 配置客户端仓库
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new CustomJdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     * 配置 OAuth2 授权确认服务
     */
    @Bean
    public OAuth2AuthorizationConsentService oauth2AuthorizationConsentService(JdbcTemplate jdbcTemplate, 
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * 配置 OAuth2 授权服务
     */
    @Bean
    public OAuth2AuthorizationService oauth2AuthorizationService(JdbcTemplate jdbcTemplate, 
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * 配置授权服务器设置
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8084")
                .build();
    }
} 