package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.TokenRequest;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.service.TokenService;
import com.double2and9.base.enums.AuthErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
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
public class TokenController {
    
    private final TokenService tokenService;

    @Operation(summary = "获取或刷新令牌", description = "支持授权码模式获取令牌和刷新令牌")
    @ApiResponse(responseCode = "200", description = "成功返回令牌")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "客户端认证失败")
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            HttpServletRequest httpRequest) {
        // 1. 从HTTP Basic认证头提取客户端凭证
        String[] clientCredentials = extractClientCredentials(httpRequest);
        
        // 2. 如果未提供客户端凭证，则返回401未授权错误
        if (clientCredentials == null) {
            throw new AuthException(AuthErrorCode.INVALID_CLIENT_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
        
        // 3. 构建 TokenRequest 对象
        TokenRequest request = new TokenRequest();
        request.setGrantType(grantType);
        request.setCode(code);
        request.setRedirectUri(redirectUri);
        request.setRefreshToken(refreshToken);
        request.setScope(scope);
        request.setCodeVerifier(codeVerifier);
        
        // 4. 验证请求参数
        validateTokenRequest(request);
        
        // 5. 调用service处理令牌请求，传入客户端凭证和请求参数
        return tokenService.createToken(clientCredentials[0], clientCredentials[1], request);
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
    
    /**
     * 验证令牌请求参数
     * 
     * @param request 令牌请求
     * @throws AuthException 如果请求参数验证失败
     */
    private void validateTokenRequest(TokenRequest request) {
        if ("authorization_code".equals(request.getGrantType())) {
            // 授权码模式验证
            if (request.getCode() == null || request.getCode().isEmpty()) {
                throw new AuthException(AuthErrorCode.PARAMETER_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
            }
            if (request.getRedirectUri() == null || request.getRedirectUri().isEmpty()) {
                throw new AuthException(AuthErrorCode.PARAMETER_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
            }
        } else if ("refresh_token".equals(request.getGrantType())) {
            // 刷新令牌模式验证
            if (request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
                throw new AuthException(AuthErrorCode.PARAMETER_VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
            }
        } else {
            throw new AuthException(AuthErrorCode.INVALID_GRANT_TYPE, HttpStatus.BAD_REQUEST);
        }
    }
} 