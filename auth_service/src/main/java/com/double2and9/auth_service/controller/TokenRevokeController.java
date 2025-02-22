package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.TokenRevokeRequest;
import com.double2and9.auth_service.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class TokenRevokeController {
    
    private final JwtService jwtService;
    
    @Operation(summary = "撤销令牌", description = "撤销访问令牌或刷新令牌")
    @ApiResponse(responseCode = "200", description = "令牌撤销成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @PostMapping("/revoke")
    public void revokeToken(@RequestBody @Valid TokenRevokeRequest request) {
        jwtService.revokeToken(request.getToken());
    }
} 