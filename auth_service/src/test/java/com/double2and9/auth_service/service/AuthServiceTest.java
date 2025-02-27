package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.request.RegisterRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.security.AuthJwtTokenProvider;
import com.double2and9.auth_service.security.SecurityUser;
import com.double2and9.base.enums.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private AuthJwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Role userRole;
    private User user;
    private Authentication authentication;

    private static final String TEST_IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        // 设置注册请求数据
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("test@example.com");

        // 设置登录请求数据
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // 设置用户角色
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        // 设置用户
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setEmail("test@example.com");
        user.setRoles(Collections.singleton(userRole));
        user.setEnabled(true);

        // 设置认证对象
        SecurityUser securityUser = new SecurityUser(user);
        authentication = new UsernamePasswordAuthenticationToken(
                securityUser, null, securityUser.getAuthorities());
    }

    @Test
    void register_Success() {
        // 配置Mock行为
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateToken(any())).thenReturn("jwt.token.here");

        // 执行注册
        AuthResponse response = authService.register(registerRequest);

        // 验证结果
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("jwt.token.here", response.getToken());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertEquals(1, response.getRoles().size());

        // 验证方法调用
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(roleRepository).findByName("ROLE_USER");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WhenUsernameExists_ThrowsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals(AuthErrorCode.USERNAME_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WhenEmailExists_ThrowsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals(AuthErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication))
                .thenReturn("test.jwt.token");
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(user));

        AuthResponse response = authService.login(loginRequest, TEST_IP);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test.jwt.token", response.getToken());
        assertTrue(response.getRoles().contains("ROLE_USER"));

        verify(userRepository).save(user);
        assertEquals(0, user.getLoginAttempts());
        assertNotNull(user.getLastLoginTime());
        assertEquals(TEST_IP, user.getLastLoginIp());
    }

    @Test
    void login_WrongPassword() {
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest, TEST_IP));

        verify(userRepository).save(user);
        assertEquals(1, user.getLoginAttempts());
    }

    @Test
    void login_AccountLocked() {
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now());
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(user));

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.login(loginRequest, TEST_IP));

        assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_LockExpired() {
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now().minusMinutes(31));
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication))
                .thenReturn("test.jwt.token");

        AuthResponse response = authService.login(loginRequest, TEST_IP);

        assertNotNull(response);
        assertFalse(user.isAccountLocked());
        assertEquals(0, user.getLoginAttempts());
        assertNull(user.getLockTime());
    }
} 