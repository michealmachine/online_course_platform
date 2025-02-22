package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.TokenIntrospectionRequest;
import com.double2and9.auth_service.dto.response.TokenIntrospectionResponse;
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
public class TokenIntrospectionController {

    private final JwtService jwtService;

    @Operation(summary = "令牌内省", description = "验证令牌并返回令牌信息")
    @ApiResponse(responseCode = "200", description = "成功返回令牌信息")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @PostMapping("/introspect")
    public TokenIntrospectionResponse introspect(@Valid @RequestBody TokenIntrospectionRequest request) {
        return jwtService.introspectToken(request.getToken());
    }
} 