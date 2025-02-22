package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AuthorizationConsentRequest {
    @NotBlank(message = "授权ID不能为空")
    private String authorizationId;
    
    @NotEmpty(message = "授权范围不能为空")
    private Set<String> approvedScopes;  // 用户同意的授权范围
} 