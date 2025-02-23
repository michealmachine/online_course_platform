package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.service.TokenService;
import com.double2and9.base.enums.AuthErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class TokenController {
    
    private final TokenService tokenService;

    @Operation(summary = "获取或刷新令牌", description = "支持授权码模式获取令牌和刷新令牌")
    @ApiResponse(responseCode = "200", description = "成功返回令牌")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "客户端认证失败")
    @PostMapping("/token")
    public TokenResponse token(@Validated @RequestBody TokenRequest request) {
        if ("authorization_code".equals(request.getGrantType())) {
            // 只有当授权类型为 authorization_code 时才验证这些字段
            if (request.getCode() == null || request.getRedirectUri() == null) {
                throw new AuthException(AuthErrorCode.PARAMETER_VALIDATION_FAILED);
            }
        } else if ("refresh_token".equals(request.getGrantType())) {
            // 刷新令牌模式验证 refreshToken
            if (request.getRefreshToken() == null) {
                throw new AuthException(AuthErrorCode.PARAMETER_VALIDATION_FAILED);
            }
        }
        return tokenService.createToken(request);
    }
} 