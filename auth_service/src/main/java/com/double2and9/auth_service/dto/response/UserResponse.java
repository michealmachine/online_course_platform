package com.double2and9.auth_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String phone;
    private String avatar;
    private boolean enabled;
    private Set<String> roles;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 