package com.double2and9.auth_service.repository;

import com.double2and9.auth_service.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    Set<Role> findAllByNameIn(Collection<String> names);
    boolean existsByName(String name);
} 