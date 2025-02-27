package com.double2and9.auth_service.config;

import com.double2and9.auth_service.security.AuthJwtTokenProvider;
import com.double2and9.base.config.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    
    @Bean
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    @Bean
    public AuthJwtTokenProvider authJwtTokenProvider(JwtProperties jwtProperties) {
        return new AuthJwtTokenProvider(jwtProperties);
    }
} 