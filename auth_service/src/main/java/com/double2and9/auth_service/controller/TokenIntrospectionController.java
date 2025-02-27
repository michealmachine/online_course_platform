package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.TokenIntrospectionRequest;
import com.double2and9.auth_service.dto.response.TokenIntrospectionResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.service.JwtService;
import com.double2and9.base.enums.AuthErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        // 从HTTP Basic认证头提取客户端凭证
        String[] clientCredentials = extractClientCredentials(httpRequest);
        
        // 如果未提供客户端凭证，则返回401未授权错误
        if (clientCredentials == null) {
            throw new AuthException(AuthErrorCode.INVALID_CLIENT_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
        
        // 如果是ID Token，使用特殊的内省逻辑
        if ("id_token".equals(request.getTokenTypeHint())) {
            return jwtService.introspectIdToken(request.getToken());
        }
        
        return jwtService.introspectToken(request.getToken());
    }

    /**
     * 从HTTP请求的Authorization头中提取客户端凭证
     * 
     * @param request HTTP请求
     * @return 包含clientId和clientSecret的数组，如果无法提取则返回null
     */
    private String[] extractClientCredentials(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.toLowerCase().startsWith("basic ")) {
            try {
                // 去掉"Basic "前缀并解码
                String base64Credentials = authHeader.substring(6);
                byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                // 凭证格式应为"clientId:clientSecret"
                final String[] values = credentials.split(":", 2);
                if (values.length == 2) {
                    return values;
                }
            } catch (Exception e) {
                // 如果解析失败，返回null
            }
        }
        return null;
    }
} 