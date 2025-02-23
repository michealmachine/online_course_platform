package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRequest {
    @NotBlank(message = "授权类型不能为空")
    private String grantType;  // 授权类型，支持 authorization_code 和 refresh_token

    @NotBlank(message = "授权码不能为空", groups = AuthorizationCodeValidation.class)
    private String code;  // 授权码

    @NotBlank(message = "重定向URI不能为空", groups = AuthorizationCodeValidation.class)
    private String redirectUri;  // 重定向URI

    @NotBlank(message = "客户端ID不能为空")
    private String clientId;  // 客户端ID

    @NotBlank(message = "客户端密钥不能为空")
    private String clientSecret;  // 客户端密钥

    @NotBlank(message = "刷新令牌不能为空", groups = RefreshTokenValidation.class)
    private String refreshToken;  // 刷新令牌，refresh_token 模式必需

    private String codeVerifier;  // PKCE验证码

    // 分组接口
    public interface AuthorizationCodeValidation {}
    public interface RefreshTokenValidation {}
} 