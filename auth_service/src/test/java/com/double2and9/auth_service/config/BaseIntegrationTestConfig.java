package com.double2and9.auth_service.config;

import com.double2and9.auth_service.AuthServiceApplication;
import com.double2and9.auth_service.repository.*;
import com.double2and9.auth_service.security.CustomUserDetailsService;
import com.double2and9.auth_service.security.JwtAuthenticationFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * 集成测试的基础配置类
 */
@TestConfiguration
@Import(AuthServiceApplication.class)
@ActiveProfiles("dev")
public class BaseIntegrationTestConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(
            HttpSecurity http,
            CustomUserDetailsService customUserDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        SecurityConfig securityConfig = new SecurityConfig(customUserDetailsService, jwtAuthenticationFilter);
        return securityConfig.securityFilterChain(http);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public BearerTokenAuthenticationEntryPoint bearerTokenAuthenticationEntryPoint() {
        return new BearerTokenAuthenticationEntryPoint();
    }

    @Bean
    public BearerTokenAccessDeniedHandler bearerTokenAccessDeniedHandler() {
        return new BearerTokenAccessDeniedHandler();
    }
} 