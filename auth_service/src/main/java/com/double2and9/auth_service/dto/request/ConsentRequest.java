package com.double2and9.auth_service.dto.request;

import lombok.Data;
import java.util.Set;

@Data
public class ConsentRequest {
    private String clientId;
    private String userId;
    private String redirectUri;
    private String scope;
    private String principal;
    private Set<String> scopes;
} 