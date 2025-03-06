package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.request.RegisterRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.security.AuthJwtTokenProvider;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.double2and9.auth_service.exception.AuthException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int MAX_LOGIN_ATTEMPTS = 5;  // 最大登录失败次数
    private static final int LOCK_DURATION_MINUTES = 30;  // 锁定时长(分钟)

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuthJwtTokenProvider tokenProvider;
    private final CaptchaService captchaService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 验证密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException(AuthErrorCode.PASSWORD_MISMATCH, HttpStatus.BAD_REQUEST);
        }

        // 验证验证码
        if (!captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            throw new AuthException(AuthErrorCode.INVALID_CAPTCHA, HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException(AuthErrorCode.USERNAME_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new AuthException(AuthErrorCode.ROLE_NOT_EXISTS, HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setEmailVerified(true); // 暂时默认邮箱已验证，后续实现验证功能
        user.setRoles(Collections.singleton(userRole));
        user.setCreatedAt(LocalDateTime.now());
        
        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        return generateAuthResponse(authentication);
    }

    public AuthResponse login(LoginRequest request, String ip) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND, HttpStatus.BAD_REQUEST));

        if (user.isAccountLocked()) {
            if (isLockExpired(user)) {
                resetLockStatus(user);
            } else {
                throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED, HttpStatus.BAD_REQUEST);
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            handleLoginSuccess(user, ip);

            return generateAuthResponse(authentication);
        } catch (BadCredentialsException e) {
            handleLoginFailure(user);
            throw e;
        }
    }

    /**
     * 使用用户名和密码直接登录（用于OAuth2流程）
     */
    public String login(String username, String password, String ip) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND, HttpStatus.BAD_REQUEST));

        if (user.isAccountLocked()) {
            if (isLockExpired(user)) {
                resetLockStatus(user);
            } else {
                throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED, HttpStatus.BAD_REQUEST);
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            handleLoginSuccess(user, ip);
            
            // 返回JWT令牌
            return tokenProvider.generateToken(authentication);
        } catch (BadCredentialsException e) {
            handleLoginFailure(user);
            throw e;
        }
    }

    private AuthResponse generateAuthResponse(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        Set<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .token(jwt)
                .username(authentication.getName())
                .roles(roles)
                .build();
    }

    private void handleLoginSuccess(User user, String ip) {
        user.setLoginAttempts(0);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userRepository.save(user);
    }

    private void handleLoginFailure(User user) {
        user.setLoginAttempts(user.getLoginAttempts() + 1);
        if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    private boolean isLockExpired(User user) {
        return user.getLockTime() != null && 
               user.getLockTime().plusMinutes(LOCK_DURATION_MINUTES).isBefore(LocalDateTime.now());
    }

    private void resetLockStatus(User user) {
        user.setAccountLocked(false);
        user.setLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
    }
} 