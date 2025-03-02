package com.double2and9.auth_service.config;

import com.double2and9.auth_service.security.CustomUserDetailsService;
import com.double2and9.auth_service.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
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
                    // 登录和授权确认页面
                    .requestMatchers(
                        "/auth/login", 
                        "/auth/register", 
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
                    // 认证相关端点
                    .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/oauth2/token",
                        "/api/oauth2/revoke",
                        "/api/oauth2/introspect"
                    ).permitAll()
                    // UserInfo 端点需要认证
                    .requestMatchers("/oauth2/userinfo").authenticated()
                    // 其他请求需要认证
                    .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exHandling -> exHandling
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"code\":\"" 
                            + HttpStatus.UNAUTHORIZED.value() 
                            + "\",\"message\":\"" 
                            + authException.getMessage() 
                            + "\"}");
                    })
                )
                .build();
    }
}