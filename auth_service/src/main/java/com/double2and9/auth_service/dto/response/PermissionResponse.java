package com.double2and9.auth_service.dto.response;

import com.double2and9.base.enums.PermissionType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PermissionResponse {
    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PermissionType type;
    private String scope;
} 