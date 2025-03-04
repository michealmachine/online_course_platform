package com.double2and9.auth_service.controller;

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
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
@Tag(name = "令牌接口", description = "令牌相关接口")
public class TokenRevokeController {
    
    private final JwtService jwtService;
    
    @Operation(
        summary = "撤销令牌", 
        description = "撤销访问令牌或刷新令牌，需要客户端认证",
        security = { @SecurityRequirement(name = "client-authentication") }
    )
    @ApiResponse(responseCode = "200", description = "令牌撤销成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "客户端认证失败")
    @PostMapping(
        value = "/revoke",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public void revokeToken(
        @RequestParam("token") String token,
        @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
        HttpServletRequest httpRequest
    ) {
        // 从HTTP Basic认证头提取客户端凭证
        String[] clientCredentials = extractClientCredentials(httpRequest);
        
        // 如果未提供客户端凭证，则返回401未授权错误
        if (clientCredentials == null) {
            throw new AuthException(AuthErrorCode.INVALID_CLIENT_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
        
        jwtService.revokeToken(token);
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