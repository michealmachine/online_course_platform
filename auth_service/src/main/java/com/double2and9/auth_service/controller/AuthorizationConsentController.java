package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.AuthorizationConsentRequest;
import com.double2and9.auth_service.dto.response.AuthorizationConsentResponse;
import com.double2and9.auth_service.service.AuthorizationConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class AuthorizationConsentController {
    
    private final AuthorizationConsentService authorizationConsentService;

    @Operation(summary = "确认授权", description = "用户确认授权请求，返回授权码")
    @ApiResponse(responseCode = "200", description = "成功返回授权码")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "用户未登录")
    @PostMapping("/consent")
    public AuthorizationConsentResponse consent(
            @Valid @RequestBody AuthorizationConsentRequest request,
            Authentication authentication) {
        return authorizationConsentService.consent(request, authentication);
    }
} 