package com.double2and9.auth_service.dto.response;

import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    
    // 机构用户字段
    private Long organizationId;
    
    // OIDC相关字段
    private String givenName;
    private String familyName;
    private String middleName;
    private String preferredUsername;
    private String profile;
    private String website;
    private String gender;
    private String birthdate;
    private String zoneinfo;
    private String locale;
    private Boolean emailVerified;
    private Boolean phoneVerified;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .givenName(user.getGivenName())
                .familyName(user.getFamilyName())
                .middleName(user.getMiddleName())
                .preferredUsername(user.getPreferredUsername())
                .profile(user.getProfile())
                .website(user.getWebsite())
                .gender(user.getGender())
                .birthdate(user.getBirthdate())
                .zoneinfo(user.getZoneinfo())
                .locale(user.getLocale())
                .build();
    }
} 