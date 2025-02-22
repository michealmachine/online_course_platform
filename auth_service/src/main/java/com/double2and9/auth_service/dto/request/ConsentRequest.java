package com.double2and9.auth_service.dto.request;

import lombok.Data;

@Data
public class ConsentRequest {
    private String clientId;
    private String userId;
    private String redirectUri;
    private String scope;
} 