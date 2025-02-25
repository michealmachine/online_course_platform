package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.AuthorizationConsentRequest;
import com.double2and9.auth_service.dto.response.AuthorizationConsentResponse;
import com.double2and9.auth_service.dto.response.AuthorizationResponse;
import com.double2and9.auth_service.service.AuthorizationConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class AuthorizationConsentController {
    
    private final AuthorizationConsentService authorizationConsentService;

    @Operation(summary = "获取授权确认页面", description = "获取待确认的授权请求信息用于显示同意页面")
    @ApiResponse(responseCode = "200", description = "成功返回授权请求信息")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "用户未登录")
    @ApiResponse(responseCode = "404", description = "授权请求不存在")
    @GetMapping("/consent")
    public AuthorizationResponse getConsentPage(
            @RequestParam("authorization_id") String authorizationId,
            Authentication authentication) {
        return authorizationConsentService.getAuthorizationRequest(authorizationId, authentication);
    }

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