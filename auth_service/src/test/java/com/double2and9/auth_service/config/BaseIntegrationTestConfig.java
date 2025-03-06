package com.double2and9.auth_service.config;

import com.double2and9.auth_service.AuthServiceApplication;
import com.double2and9.auth_service.repository.*;
import com.double2and9.auth_service.security.CustomUserDetailsService;
import com.double2and9.auth_service.security.JwtAuthenticationFilter;
import com.double2and9.auth_service.service.MockTokenBlacklistService;
import com.double2and9.auth_service.service.TokenBlacklistService;
import com.double2and9.base.config.JwtProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * 集成测试的基础配置类
 */
@TestConfiguration
@Import(AuthServiceApplication.class)
@ActiveProfiles("dev")
public class BaseIntegrationTestConfig {

    /**
     * 配置测试环境的OAuth2授权服务器安全过滤链
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain testAuthorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        // 应用默认的OAuth2授权服务器安全配置
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        // 自定义授权服务器配置
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            // 配置授权端点
            .authorizationEndpoint(authorizationEndpoint -> 
                // 设置自定义授权确认页面路径
                authorizationEndpoint.consentPage("/oauth2/consent"))
            // 添加OIDC（OpenID Connect）支持
            .oidc(Customizer.withDefaults());
        
        // 配置未认证请求的处理方式
        http
            // 所有未认证的请求都重定向到登录页
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/auth/login")))
            // 配置资源服务器
            .oauth2ResourceServer(oauth2ResourceServer ->
                oauth2ResourceServer.jwt(Customizer.withDefaults()));
                
        // 添加调试信息
        System.out.println("测试环境OAuth2授权服务器过滤链已配置，未认证请求会重定向到/auth/login");
            
        return http.build();
    }

    /**
     * 配置测试环境的默认安全过滤链
     */
    @Bean
    @Order(2)
    public SecurityFilterChain testDefaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    // 静态资源
                    .requestMatchers(
                        "/css/**", 
                        "/js/**", 
                        "/images/**", 
                        "/webjars/**", 
                        "/error",
                        "/favicon.ico"
                    ).permitAll()
                    // 登录、注册和授权确认页面
                    .requestMatchers(
                        "/auth/login", 
                        "/auth/register", 
                        "/auth/captcha", 
                        "/oauth2/consent"
                    ).permitAll()
                    // Swagger UI 和 OpenAPI 相关路径
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources",
                        "/swagger-resources/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/webjars/**",
                        "/doc.html"
                    ).permitAll()
                    // OpenID Connect 发现端点
                    .requestMatchers(
                        "/.well-known/openid-configuration",
                        "/.well-known/jwks.json",
                        "/oauth2/.well-known/openid-configuration",
                        "/oauth2/.well-known/jwks.json",
                        "/oauth2/jwks",
                        "/oauth2/check-session",
                        "/oauth2/end-session",
                        "/oauth2/session/end"
                    ).permitAll()
                    // 其他请求需要认证
                    .anyRequest().authenticated()
                )
                .formLogin(formLogin -> formLogin
                    .loginPage("/auth/login")
                    .loginProcessingUrl("/auth/login")
                    .permitAll()
                )
                .build();
    }

    /**
     * 配置JWT认证转换器
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    /**
     * 配置Bearer令牌认证入口点
     */
    @Bean
    public BearerTokenAuthenticationEntryPoint bearerTokenAuthenticationEntryPoint() {
        return new BearerTokenAuthenticationEntryPoint();
    }

    /**
     * 配置Bearer令牌访问拒绝处理器
     */
    @Bean
    public BearerTokenAccessDeniedHandler bearerTokenAccessDeniedHandler() {
        return new BearerTokenAccessDeniedHandler();
    }
    
    /**
     * 为测试环境提供令牌黑名单服务的内存实现
     * 避免在测试中依赖Redis
     */
    @Bean
    @Primary
    public TokenBlacklistService testTokenBlacklistService() {
        // 使用专门为测试创建的模拟实现
        return new MockTokenBlacklistService();
    }

    /**
     * 为测试环境提供AuthJwtTokenProvider
     * 确保使用相同的密钥进行JWT验证
     */
    @Bean
    @Primary
    public com.double2and9.auth_service.security.AuthJwtTokenProvider testAuthJwtTokenProvider(JwtProperties jwtProperties) {
        jwtProperties.setSecret(TestSecurityConfig.DEV_SECRET);
        System.out.println("测试环境AuthJwtTokenProvider初始化 - 使用密钥: " + jwtProperties.getSecret().substring(0, 10) + "...");
        return new com.double2and9.auth_service.security.AuthJwtTokenProvider(jwtProperties);
    }
} 