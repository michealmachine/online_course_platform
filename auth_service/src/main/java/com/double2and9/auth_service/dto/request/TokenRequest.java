package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Schema(description = "令牌请求")
public class TokenRequest {
    @Schema(description = "授权类型", example = "authorization_code")
    @NotBlank(message = "授权类型不能为空")
    @JsonProperty("grant_type")
    private String grantType;  // 授权类型，支持 authorization_code 和 refresh_token

    // authorization_code 模式需要的字段
    @Schema(description = "授权码", example = "abc123")
    @JsonProperty("code")
    private String code;  // 授权码
    @Schema(description = "重定向URI", example = "http://localhost:8080/callback")
    @JsonProperty("redirect_uri")
    private String redirectUri;  // 重定向URI
    @Schema(description = "随机字符串", example = "xyz789")
    @JsonProperty("nonce")
    private String nonce;  // OIDC nonce参数
    
    // refresh_token 模式需要的字段
    @Schema(description = "刷新令牌", example = "refresh.token.xyz")
    @JsonProperty("refresh_token")
    private String refreshToken;  // 刷新令牌
    
    // PKCE 扩展需要的字段
    @Schema(description = "授权范围", example = "openid profile email")
    @JsonProperty("scope")
    private String scope;
    @Schema(description = "PKCE验证码", example = "code_verifier_123")
    @JsonProperty("code_verifier")
    private String codeVerifier;  // PKCE验证码

    public TokenRequest() {}
} 