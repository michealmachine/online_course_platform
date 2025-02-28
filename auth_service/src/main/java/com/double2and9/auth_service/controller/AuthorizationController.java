package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.AuthorizationRequest;
import com.double2and9.auth_service.dto.response.AuthorizationResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.service.AuthorizationService;
import com.double2and9.auth_service.service.OidcAuthorizationService;
import com.double2and9.base.enums.AuthErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class AuthorizationController {
    
    private final AuthorizationService authorizationService;
    private final OidcAuthorizationService oidcAuthorizationService;

    @Operation(summary = "授权请求", description = "处理客户端的授权请求，返回授权信息供用户确认")
    @ApiResponse(responseCode = "200", description = "成功返回授权信息")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "用户未登录")
    @PostMapping("/authorize")
    @ResponseStatus(HttpStatus.OK)
    public AuthorizationResponse authorize(
            @Valid @RequestBody AuthorizationRequest request,
            Authentication authentication) {
        try {
            // 如果scope包含openid，进行OIDC相关验证
            if (request.getScope() != null && request.getScope().contains("openid")) {
                oidcAuthorizationService.validateAuthorizationRequest(
                    request.getClientId(),
                    request.getNonce(),
                    request.getScope()
                );
            }
            return authorizationService.createAuthorizationRequest(request, authentication);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_REQUEST);
        }
    }
    
    @Operation(summary = "授权请求(GET)", description = "使用GET方法处理客户端的授权请求，返回授权信息供用户确认")
    @ApiResponse(responseCode = "200", description = "成功返回授权信息")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "用户未登录")
    @GetMapping("/authorize")
    @ResponseStatus(HttpStatus.OK)
    public AuthorizationResponse authorizeGet(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("scope") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            Authentication authentication) {
        try {
            // 如果scope包含openid，进行OIDC相关验证
            if (scope != null && scope.contains("openid")) {
                oidcAuthorizationService.validateAuthorizationRequest(clientId, nonce, scope);
            }
            
            // 将查询参数转换为AuthorizationRequest对象
            AuthorizationRequest request = new AuthorizationRequest();
            request.setResponseType(responseType);
            request.setClientId(clientId);
            request.setRedirectUri(redirectUri);
            request.setScope(scope);
            request.setState(state);
            request.setNonce(nonce);
            request.setCodeChallenge(codeChallenge);
            request.setCodeChallengeMethod(codeChallengeMethod);
            
            return authorizationService.createAuthorizationRequest(request, authentication);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_REQUEST);
        }
    }
} 