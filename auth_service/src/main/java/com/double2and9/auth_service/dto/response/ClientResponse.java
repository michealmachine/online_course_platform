package com.double2and9.auth_service.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class ClientResponse {
    private String id;
    private String clientId;
    private Instant clientIdIssuedAt;
    private String clientName;
    private Set<String> authenticationMethods;
    private Set<String> authorizationGrantTypes;
    private Set<String> redirectUris;
    private Set<String> scopes;
} 