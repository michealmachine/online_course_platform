package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.TokenIntrospectionRequest;
import com.double2and9.auth_service.dto.response.TokenIntrospectionResponse;
import com.double2and9.auth_service.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
@Tag(name = "令牌接口", description = "令牌相关接口")
public class TokenIntrospectionController {

    private final JwtService jwtService;

    @Operation(
        summary = "令牌内省", 
        description = "验证令牌并返回令牌信息，需要客户端认证",
        security = { @SecurityRequirement(name = "client-authentication") }
    )
    @ApiResponse(responseCode = "200", description = "成功返回令牌信息")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "客户端认证失败")
    @PostMapping("/introspect")
    public TokenIntrospectionResponse introspect(@Valid @RequestBody TokenIntrospectionRequest request, HttpServletRequest httpRequest) {
        // 这里可以添加客户端认证逻辑，类似于TokenController中的实现
        return jwtService.introspectToken(request.getToken());
    }
} 