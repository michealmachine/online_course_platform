package com.double2and9.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    private boolean enabled = true;

    @Getter
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String nickname;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 255)
    private String avatar;

    @Column(name = "account_locked")
    private boolean accountLocked = false;
    
    @Column(name = "login_attempts")
    private int loginAttempts = 0;
    
    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
    
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "preferred_username")
    private String preferredUsername;

    @Column(name = "profile")
    private String profile;

    @Column(name = "website")
    private String website;

    @Column(name = "gender")
    private String gender;

    @Column(name = "birthdate")
    private String birthdate;

    @Column(name = "zoneinfo")
    private String zoneinfo;

    @Column(name = "locale")
    private String locale;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    }

    @Transient
    public boolean isOrganizationUser() {
        return roles.stream()
                .anyMatch(role -> role.getName().equals("ROLE_ORGANIZATION"));
    }
} 