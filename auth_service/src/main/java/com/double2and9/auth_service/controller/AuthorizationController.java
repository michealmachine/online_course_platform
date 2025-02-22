package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.AuthorizationRequest;
import com.double2and9.auth_service.dto.response.AuthorizationResponse;
import com.double2and9.auth_service.service.AuthorizationService;
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

    @Operation(summary = "授权请求", description = "处理客户端的授权请求，返回授权信息供用户确认")
    @ApiResponse(responseCode = "200", description = "成功返回授权信息")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "用户未登录")
    @PostMapping("/authorize")
    @ResponseStatus(HttpStatus.OK)
    public AuthorizationResponse authorize(
            @Valid @RequestBody AuthorizationRequest request,
            Authentication authentication) {
        return authorizationService.createAuthorizationRequest(request, authentication);
    }
} 