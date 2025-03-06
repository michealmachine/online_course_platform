package com.double2and9.auth_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String username;
    private Set<String> roles;
    private String refreshToken;
    private String idToken;
    private String tokenType;
    private Long expiresIn;
    private String scope;
    private String jti;
} 