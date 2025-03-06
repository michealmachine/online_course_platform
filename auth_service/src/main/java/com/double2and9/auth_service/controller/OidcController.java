package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.config.OidcConfig;
import com.double2and9.auth_service.dto.response.TokenIntrospectionResponse;
import com.double2and9.auth_service.dto.response.UserInfoResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.service.JwtService;
import com.double2and9.auth_service.service.UserService;
import com.double2and9.auth_service.service.OidcSessionService;
import com.double2and9.base.enums.AuthErrorCode;
import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * OIDC扩展控制器 
 * 提供标准OAuth2/OIDC端点之外的附加功能
 */
@Tag(name = "OIDC扩展", description = "OpenID Connect额外扩展端点")
@RestController
@RequestMapping("/api/oidc")
@RequiredArgsConstructor
public class OidcController {

    private final OidcConfig oidcConfig;
    private final UserService userService;
    private final JwtService jwtService;
    private final JWKSet jwkSet;
    private final OidcSessionService sessionService;

    @Operation(summary = "获取用户信息", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/userinfo")
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {
        // 从请求头中获取令牌
        String token = null;
        if (authentication.getCredentials() != null) {
            token = authentication.getCredentials().toString();
        }
        
        // 如果没有令牌，抛出未授权异常
        if (token == null) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_FAILED, HttpStatus.UNAUTHORIZED);
        }
        
        // 检查令牌是否被撤销
        TokenIntrospectionResponse introspectionResponse = jwtService.introspectToken(token);
        if (!introspectionResponse.isActive()) {
            throw new AuthException(AuthErrorCode.TOKEN_REVOKED, HttpStatus.UNAUTHORIZED);
        }
        
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
        config.put("check_session_iframe", issuer + "/oauth2/check-session");
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

    /**
     * 检查会话状态API
     * 注意：标准的会话检查已由OAuth2框架处理，这是一个扩展功能
     */
    @Operation(summary = "检查会话状态(扩展)")
    @GetMapping("/check-session")
    public ResponseEntity<String> checkSession(
            @RequestParam("client_id") String clientId,
            @RequestParam("session_state") String sessionState) {
        return ResponseEntity.ok(String.valueOf(sessionService.checkSession(clientId, sessionState)));
    }

    /**
     * 自定义会话结束API
     * 注意：标准的登出流程已由OAuth2框架处理，这是一个扩展功能
     */
    @Operation(summary = "自定义结束会话(扩展)")
    @GetMapping("/custom-end-session")
    public ResponseEntity<Void> endSession(
            @RequestParam(required = false) String idTokenHint,
            @RequestParam(required = false) String postLogoutRedirectUri,
            @RequestParam(required = false) String state) {
        sessionService.endSession(idTokenHint, postLogoutRedirectUri, state);
        return ResponseEntity.ok().build();
    }
} 