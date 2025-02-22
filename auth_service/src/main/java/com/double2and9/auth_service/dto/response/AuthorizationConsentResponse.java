package com.double2and9.auth_service.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationConsentResponse {
    private String authorizationCode;  // 授权码
    private String state;              // 原始请求中的state
    private String redirectUri;        // 重定向URI
} 