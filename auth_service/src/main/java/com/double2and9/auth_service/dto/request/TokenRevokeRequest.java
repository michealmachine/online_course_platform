package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRevokeRequest {
    @NotBlank(message = "令牌不能为空")
    private String token;  // 要撤销的令牌
    
    private String tokenTypeHint;  // 令牌类型提示（可选）：access_token 或 refresh_token
} 