package com.double2and9.auth_service.config;

import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
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
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Map;
import java.util.function.Function;
import java.time.Instant;
import java.util.HashMap;
import org.springframework.util.StringUtils;

/**
 * OAuth2 授权服务器配置
 * 使用Spring Security标准的OAuth2授权服务器实现
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    /**
     * 配置授权服务器的安全过滤链
     * 该过滤链负责处理所有OAuth2相关端点
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        // 应用默认的OAuth2授权服务器安全配置
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        // 自定义授权服务器配置
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            // 配置授权端点
            .authorizationEndpoint(authorizationEndpoint -> 
                // 设置自定义授权确认页面路径
                authorizationEndpoint.consentPage("/oauth2/consent"))
            // 配置令牌端点
            .tokenEndpoint(tokenEndpoint ->
                tokenEndpoint.accessTokenRequestConverter(accessTokenRequestConverter())
                    .accessTokenResponseHandler(accessTokenResponseHandler()))
            // 配置令牌内省端点
            .tokenIntrospectionEndpoint(introspection -> 
                introspection.introspectionResponseHandler(introspectionResponseHandler()))
            // 配置令牌撤销端点
            .tokenRevocationEndpoint(revocation -> 
                revocation.revocationResponseHandler(revocationResponseHandler()))
            // 添加OIDC（OpenID Connect）支持
            .oidc(oidcConfigurer -> 
                oidcConfigurer
                    .userInfoEndpoint(userinfo -> userinfo.userInfoMapper(userInfoMapper()))
                    .logoutEndpoint(logout -> logout.logoutResponseHandler(oidcLogoutSuccessHandler())));
        
        // 配置未认证请求的处理方式
        http
            // 所有未认证的请求都重定向到登录页
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/auth/login")))
            // 配置资源服务器使用JWT验证
            .oauth2ResourceServer(oauth2ResourceServer ->
                oauth2ResourceServer.jwt(Customizer.withDefaults()));
            
        return http.build();
    }

    /**
     * 配置客户端仓库
     * 负责管理OAuth2客户端的注册信息
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new CustomJdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     * 配置OAuth2授权确认服务
     * 负责管理用户对客户端的授权确认记录
     */
    @Bean
    public OAuth2AuthorizationConsentService oauth2AuthorizationConsentService(JdbcTemplate jdbcTemplate, 
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * 配置OAuth2授权服务
     * 负责管理OAuth2授权信息，包括授权码、访问令牌等
     */
    @Bean
    public OAuth2AuthorizationService oauth2AuthorizationService(JdbcTemplate jdbcTemplate, 
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * 配置授权服务器设置
     * 设置授权服务器的基本属性，如发行方（issuer）URL
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8084")
                .build();
    }

    /**
     * 配置访问令牌请求转换器
     */
    @Bean
    public AuthenticationConverter accessTokenRequestConverter() {
        return request -> {
            // 默认实现，可根据需要自定义
            return null;
        };
    }

    /**
     * 配置访问令牌响应处理器
     */
    @Bean
    public AuthenticationSuccessHandler accessTokenResponseHandler() {
        return (request, response, authentication) -> {
            // 在这里你可以添加自定义逻辑处理令牌响应
        };
    }

    /**
     * 配置令牌内省响应处理器
     */
    @Bean
    public AuthenticationSuccessHandler introspectionResponseHandler() {
        return (request, response, authentication) -> {
            // 在这里你可以添加自定义逻辑处理内省响应
        };
    }

    /**
     * 配置令牌撤销响应处理器
     */
    @Bean
    public AuthenticationSuccessHandler revocationResponseHandler() {
        return (request, response, authentication) -> {
            // 在这里你可以添加自定义逻辑处理撤销响应
        };
    }

    /**
     * 配置用户信息映射器
     */
    @Bean
    public Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper() {
        return context -> {
            Authentication principal = context.getAuthentication();
            Map<String, Object> claims = new HashMap<>();
            
            if (principal instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) principal;
                claims.putAll(jwtToken.getToken().getClaims());
            }
            
            // 补充其他用户信息
            claims.put("updated_at", Instant.now().getEpochSecond());
            
            return new OidcUserInfo(claims);
        };
    }

    /**
     * 配置OIDC登出成功处理器
     */
    @Bean
    public AuthenticationSuccessHandler oidcLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            // 在这里你可以添加自定义逻辑处理OIDC登出
            String redirectUri = request.getParameter("post_logout_redirect_uri");
            if (StringUtils.hasText(redirectUri)) {
                response.sendRedirect(redirectUri);
            }
        };
    }
} 