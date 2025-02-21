package com.double2and9.auth_service.service;

import com.double2and9.auth_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseInitService {
    
    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Checking database initialization status...");
        
        // 验证基础角色是否存在
        roleRepository.findByName("ROLE_USER").ifPresentOrElse(
            role -> log.info("Default role ROLE_USER exists"),
            () -> log.error("Default role ROLE_USER not found!")
        );
    }
} 