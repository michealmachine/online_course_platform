package com.double2and9.auth_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "oidc")
public class OidcConfig {
    private String issuer = "http://localhost:8084";
    private String authorizationEndpoint = "/oauth2/authorize";
    private String tokenEndpoint = "/oauth2/token";
    private String userinfoEndpoint = "/api/oauth2/userinfo";
    private String jwksUri = "/oauth2/jwks";
    private String endSessionEndpoint = "/oauth2/logout";
    private String[] scopesSupported = {"openid", "profile", "email", "phone", "address"};
    private String[] responseTypesSupported = {"code"};
    private String[] subjectTypesSupported = {"public"};
    private String[] idTokenSigningAlgValuesSupported = {"RS256"};
    private String[] claimsSupported = {
        "sub", "iss", "aud", "exp", "iat",
        "name", "family_name", "given_name", "middle_name",
        "nickname", "preferred_username", "profile",
        "picture", "website", "gender", "birthdate",
        "zoneinfo", "locale", "updated_at",
        "email", "email_verified",
        "phone_number", "phone_number_verified"
    };
} 