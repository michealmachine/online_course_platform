package com.double2and9.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    }
} 