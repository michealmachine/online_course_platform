package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.CreateUserRequest;
import com.double2and9.auth_service.dto.request.UpdateUserRequest;
import com.double2and9.auth_service.dto.response.UserInfoResponse;
import com.double2and9.auth_service.dto.response.UserResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.mapper.UserMapper;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.base.enums.AuthErrorCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        
        // 更新基本信息
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        
        // 更新 OIDC 相关字段
        user.setGivenName(request.getGivenName());
        user.setFamilyName(request.getFamilyName());
        user.setMiddleName(request.getMiddleName());
        user.setPreferredUsername(request.getPreferredUsername());
        user.setGender(request.getGender());
        user.setBirthdate(request.getBirthdate());
        user.setZoneinfo(request.getZoneinfo());
        user.setLocale(request.getLocale());
        
        user = userRepository.save(user);
        return UserResponse.fromUser(user);
    }

    public Page<UserResponse> getUsers(int page, int size, String username, String email) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (username != null) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (email != null) {
                predicates.add(cb.like(root.get("email"), "%" + email + "%"));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return userRepository.findAll(spec, PageRequest.of(page, size))
                .map(userMapper::toUserResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException(AuthErrorCode.USERNAME_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(AuthErrorCode.EMAIL_EXISTS, HttpStatus.BAD_REQUEST);
        }

        // 验证角色是否存在
        Set<Role> roles = roleRepository.findAllByNameIn(request.getRoles());
        if (roles.size() != request.getRoles().size()) {
            throw new AuthException(AuthErrorCode.INVALID_ROLE, HttpStatus.BAD_REQUEST);
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEnabled(true);
        
        // 设置 OIDC 相关字段
        user.setGivenName(request.getGivenName());
        user.setFamilyName(request.getFamilyName());
        user.setMiddleName(request.getMiddleName());
        user.setPreferredUsername(request.getPreferredUsername());
        user.setProfile(request.getProfile());
        user.setWebsite(request.getWebsite());
        user.setGender(request.getGender());
        user.setBirthdate(request.getBirthdate());
        user.setZoneinfo(request.getZoneinfo());
        user.setLocale(request.getLocale());
        
        user.setRoles(roles);
        
        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        return UserInfoResponse.builder()
                .sub(user.getId().toString())
                .name(buildFullName(user))
                .givenName(user.getGivenName())
                .familyName(user.getFamilyName())
                .middleName(user.getMiddleName())
                .nickname(user.getNickname())
                .preferredUsername(user.getPreferredUsername())
                .profile(user.getProfile())
                .picture(user.getAvatar())
                .website(user.getWebsite())
                .email(user.getEmail())
                .emailVerified(user.getEmailVerified())
                .gender(user.getGender())
                .birthdate(user.getBirthdate())
                .zoneinfo(user.getZoneinfo())
                .locale(user.getLocale())
                .phoneNumber(user.getPhone())
                .phoneNumberVerified(user.getPhoneVerified())
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toEpochSecond(ZoneOffset.UTC) : null)
                .organizationId(user.isOrganizationUser() ? user.getOrganizationId() : null)
                .build();
    }

    public UserInfoResponse getUserInfoByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        return UserInfoResponse.builder()
                .sub(user.getId().toString())
                .name(buildFullName(user))
                .givenName(user.getGivenName())
                .familyName(user.getFamilyName())
                .middleName(user.getMiddleName())
                .nickname(user.getNickname())
                .preferredUsername(user.getPreferredUsername())
                .profile(user.getProfile())
                .picture(user.getAvatar())
                .website(user.getWebsite())
                .email(user.getEmail())
                .emailVerified(user.getEmailVerified())
                .gender(user.getGender())
                .birthdate(user.getBirthdate())
                .zoneinfo(user.getZoneinfo())
                .locale(user.getLocale())
                .phoneNumber(user.getPhone())
                .phoneNumberVerified(user.getPhoneVerified())
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toEpochSecond(ZoneOffset.UTC) : null)
                .organizationId(user.isOrganizationUser() ? user.getOrganizationId() : null)
                .build();
    }

    private String buildFullName(User user) {
        StringBuilder fullName = new StringBuilder();
        if (user.getGivenName() != null) {
            fullName.append(user.getGivenName());
        }
        if (user.getMiddleName() != null) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(user.getMiddleName());
        }
        if (user.getFamilyName() != null) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(user.getFamilyName());
        }
        return fullName.length() > 0 ? fullName.toString() : null;
    }
} 