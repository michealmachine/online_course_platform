package com.double2and9.auth_service.entity;

import com.double2and9.base.enums.PermissionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private String action;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PermissionType type = PermissionType.API;  // 默认值为API

    @Column(name = "scope")
    private String scope;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission)) return false;
        Permission that = (Permission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 