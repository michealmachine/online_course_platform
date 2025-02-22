package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenIntrospectionRequest {
    @NotBlank(message = "令牌不能为空")
    private String token;
    
    private String tokenTypeHint; // 可选，用于提示令牌类型
} 