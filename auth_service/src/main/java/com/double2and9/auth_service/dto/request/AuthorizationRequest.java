package com.double2and9.auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "授权请求参数")
public class AuthorizationRequest {
    @NotBlank(message = "response_type不能为空")
    @Schema(description = "响应类型", example = "code")
    private String responseType;  // 必须是 "code"

    @NotBlank(message = "client_id不能为空")
    @Schema(description = "客户端ID")
    private String clientId;

    @NotBlank(message = "redirect_uri不能为空")
    @Schema(description = "重定向URI")
    private String redirectUri;

    @NotBlank(message = "scope不能为空")
    @Schema(description = "授权范围", example = "openid profile email")
    private String scope;

    @Schema(description = "状态值，用于防止CSRF攻击")
    private String state;  // 可选，用于防止CSRF攻击

    @Schema(description = "用于防止重放攻击的随机字符串")
    private String nonce;

    // PKCE 相关字段
    @Schema(description = "PKCE挑战码")
    @Pattern(regexp = "^[A-Za-z0-9-._~]{43,128}$", message = "code_challenge 格式不正确")
    private String codeChallenge;
    
    @Schema(description = "PKCE挑战码方法")
    @Pattern(regexp = "^(plain|S256)$", message = "code_challenge_method 必须是 plain 或 S256")
    private String codeChallengeMethod = "S256"; // 默认使用 S256
} 