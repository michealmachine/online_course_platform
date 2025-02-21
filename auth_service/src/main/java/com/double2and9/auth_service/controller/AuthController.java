package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.request.RegisterRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口", description = "包含登录、注册等认证相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    @Operation(summary = "用户注册", description = "注册新用户并返回访问令牌")
    @ApiResponse(responseCode = "200", description = "注册成功")
    @ApiResponse(responseCode = "400", description = "注册失败，用户名或邮箱已存在")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "用户登录", description = "使用用户名和密码登录并返回访问令牌")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @ApiResponse(responseCode = "401", description = "登录失败，用户名或密码错误")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        return ResponseEntity.ok(authService.login(request, clientIp));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
} 