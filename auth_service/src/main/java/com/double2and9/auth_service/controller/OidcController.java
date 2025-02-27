package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.config.OidcConfig;
import com.double2and9.auth_service.dto.response.UserInfoResponse;
import com.double2and9.auth_service.service.JwtService;
import com.double2and9.auth_service.service.UserService;
import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "OIDC", description = "OpenID Connect endpoints")
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OidcController {

    private final OidcConfig oidcConfig;
    private final UserService userService;
    private final JwtService jwtService;
    private final JWKSet jwkSet;

    @Operation(summary = "获取用户信息", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/userinfo")
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.getUserInfoByUsername(username));
    }

    @Operation(summary = "获取OpenID Provider配置")
    @GetMapping("/.well-known/openid-configuration")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        String issuer = oidcConfig.getIssuer();
        config.put("issuer", issuer);
        config.put("authorization_endpoint", issuer + oidcConfig.getAuthorizationEndpoint());
        config.put("token_endpoint", issuer + oidcConfig.getTokenEndpoint());
        config.put("userinfo_endpoint", issuer + oidcConfig.getUserinfoEndpoint());
        config.put("jwks_uri", issuer + oidcConfig.getJwksUri());
        config.put("end_session_endpoint", issuer + oidcConfig.getEndSessionEndpoint());
        config.put("scopes_supported", Arrays.asList(oidcConfig.getScopesSupported()));
        config.put("response_types_supported", Arrays.asList(oidcConfig.getResponseTypesSupported()));
        config.put("subject_types_supported", Arrays.asList(oidcConfig.getSubjectTypesSupported()));
        config.put("id_token_signing_alg_values_supported", Arrays.asList(oidcConfig.getIdTokenSigningAlgValuesSupported()));
        config.put("claims_supported", Arrays.asList(oidcConfig.getClaimsSupported()));
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "获取JWKS")
    @GetMapping("/jwks")
    public ResponseEntity<JWKSet> getJwks() {
        return ResponseEntity.ok(jwkSet);
    }
} 