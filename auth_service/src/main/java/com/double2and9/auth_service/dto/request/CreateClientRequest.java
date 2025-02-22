package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class CreateClientRequest {
    @NotBlank(message = "客户端ID不能为空")
    private String clientId;
    
    @NotBlank(message = "客户端密钥不能为空")
    private String clientSecret;
    
    @NotBlank(message = "客户端名称不能为空")
    private String clientName;
    
    @NotEmpty(message = "认证方式不能为空")
    private Set<String> authenticationMethods;
    
    @NotEmpty(message = "授权类型不能为空")
    private Set<String> authorizationGrantTypes;
    
    private Set<String> redirectUris;
    
    @NotEmpty(message = "授权范围不能为空")
    private Set<String> scopes;
} 