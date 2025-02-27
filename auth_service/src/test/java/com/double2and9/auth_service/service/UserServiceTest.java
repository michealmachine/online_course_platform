package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.UpdateUserRequest;
import com.double2and9.auth_service.dto.response.UserInfoResponse;
import com.double2and9.auth_service.dto.response.UserResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.mapper.UserMapper;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.base.enums.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserResponse userResponse;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setGivenName("John");
        testUser.setFamilyName("Doe");
        testUser.setMiddleName("M");
        testUser.setPreferredUsername("johndoe");
        testUser.setProfile("http://example.com/johndoe");
        testUser.setWebsite("http://johndoe.com");
        testUser.setGender("male");
        testUser.setBirthdate("1990-01-01");
        testUser.setZoneinfo("Asia/Shanghai");
        testUser.setLocale("zh-CN");

        userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .givenName("John")
                .familyName("Doe")
                .middleName("M")
                .preferredUsername("johndoe")
                .profile("http://example.com/johndoe")
                .website("http://johndoe.com")
                .gender("male")
                .birthdate("1990-01-01")
                .zoneinfo("Asia/Shanghai")
                .locale("zh-CN")
                .build();

        updateRequest = new UpdateUserRequest();
        updateRequest.setNickname("新昵称");
        updateRequest.setPhone("13800138000");
        updateRequest.setGivenName("Jane");
        updateRequest.setFamilyName("Smith");
        updateRequest.setMiddleName("A");
        updateRequest.setPreferredUsername("janesmith");
        updateRequest.setGender("female");
        updateRequest.setBirthdate("1992-02-02");
        updateRequest.setZoneinfo("America/New_York");
        updateRequest.setLocale("en-US");
    }

    @Test
    void getUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

        UserResponse result = userService.getUser(1L);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        
        verify(userRepository).findById(1L);
        verify(userMapper).toUserResponse(testUser);
    }

    @Test
    void getUser_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> userService.getUser(1L));

        assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository).findById(1L);
        verify(userMapper, never()).toUserResponse(any());
    }

    @Test
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = userService.updateUser(1L, updateRequest);

        assertNotNull(result);
        assertEquals("新昵称", testUser.getNickname());
        assertEquals("13800138000", testUser.getPhone());
        
        // 验证 OIDC 字段更新
        assertEquals("Jane", testUser.getGivenName());
        assertEquals("Smith", testUser.getFamilyName());
        assertEquals("A", testUser.getMiddleName());
        assertEquals("janesmith", testUser.getPreferredUsername());
        assertEquals("female", testUser.getGender());
        assertEquals("1992-02-02", testUser.getBirthdate());
        assertEquals("America/New_York", testUser.getZoneinfo());
        assertEquals("en-US", testUser.getLocale());
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void getUsers_Success() {
        Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(userPage);
        when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getUsers(0, 10, "test", "test@example.com");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(userMapper).toUserResponse(testUser);
    }

    @Test
    void getUserInfo_Success() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setEmailVerified(true);
        user.setGivenName("John");
        user.setFamilyName("Doe");
        user.setMiddleName("M");
        user.setPreferredUsername("johndoe");
        user.setProfile("http://example.com/johndoe");
        user.setWebsite("http://johndoe.com");
        user.setGender("male");
        user.setBirthdate("1990-01-01");
        user.setZoneinfo("Asia/Shanghai");
        user.setLocale("zh-CN");
        user.setPhone("1234567890");
        user.setPhoneVerified(true);
        user.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // 执行测试
        UserInfoResponse response = userService.getUserInfo(1L);

        // 验证结果
        assertNotNull(response);
        assertEquals("1", response.getSub());
        assertEquals("John", response.getGivenName());
        assertEquals("Doe", response.getFamilyName());
        assertEquals("M", response.getMiddleName());
        assertEquals("johndoe", response.getPreferredUsername());
        assertEquals("http://example.com/johndoe", response.getProfile());
        assertEquals("http://johndoe.com", response.getWebsite());
        assertEquals("male", response.getGender());
        assertEquals("1990-01-01", response.getBirthdate());
        assertEquals("Asia/Shanghai", response.getZoneinfo());
        assertEquals("zh-CN", response.getLocale());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getEmailVerified());
        assertEquals("1234567890", response.getPhoneNumber());
        assertTrue(response.getPhoneNumberVerified());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void getUserInfo_WithOrganizationUser() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        user.setUsername("orguser");
        user.setEmail("org@example.com");
        
        Role orgRole = new Role();
        orgRole.setName("ROLE_ORGANIZATION");
        user.setRoles(Set.of(orgRole));
        user.setOrganizationId(123L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // 执行测试
        UserInfoResponse response = userService.getUserInfo(1L);

        // 验证结果
        assertNotNull(response);
        assertEquals("1", response.getSub());
        assertEquals("org@example.com", response.getEmail());
        assertEquals(123L, response.getOrganizationId());
    }

    @Test
    void getUserInfo_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> userService.getUserInfo(1L));

        assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getUserInfoByUsername_Success() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setEmailVerified(true);
        user.setGivenName("John");
        user.setFamilyName("Doe");
        user.setMiddleName("M");
        user.setPreferredUsername("johndoe");
        user.setProfile("http://example.com/johndoe");
        user.setWebsite("http://johndoe.com");
        user.setGender("male");
        user.setBirthdate("1990-01-01");
        user.setZoneinfo("Asia/Shanghai");
        user.setLocale("zh-CN");
        user.setPhone("1234567890");
        user.setPhoneVerified(true);
        user.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // 执行测试
        UserInfoResponse response = userService.getUserInfoByUsername("testuser");

        // 验证结果
        assertNotNull(response);
        assertEquals("1", response.getSub());
        assertEquals("John M Doe", response.getName());
        assertEquals("John", response.getGivenName());
        assertEquals("Doe", response.getFamilyName());
        assertEquals("M", response.getMiddleName());
        assertEquals("johndoe", response.getPreferredUsername());
        assertEquals("http://example.com/johndoe", response.getProfile());
        assertEquals("http://johndoe.com", response.getWebsite());
        assertEquals("male", response.getGender());
        assertEquals("1990-01-01", response.getBirthdate());
        assertEquals("Asia/Shanghai", response.getZoneinfo());
        assertEquals("zh-CN", response.getLocale());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getEmailVerified());
        assertEquals("1234567890", response.getPhoneNumber());
        assertTrue(response.getPhoneNumberVerified());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void getUserInfoByUsername_WithOrganizationUser() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        user.setUsername("orguser");
        user.setEmail("org@example.com");
        
        Role orgRole = new Role();
        orgRole.setName("ROLE_ORGANIZATION");
        user.setRoles(Set.of(orgRole));
        user.setOrganizationId(123L);

        when(userRepository.findByUsername("orguser")).thenReturn(Optional.of(user));

        // 执行测试
        UserInfoResponse response = userService.getUserInfoByUsername("orguser");

        // 验证结果
        assertNotNull(response);
        assertEquals("1", response.getSub());
        assertEquals("org@example.com", response.getEmail());
        assertEquals(123L, response.getOrganizationId());
    }

    @Test
    void getUserInfoByUsername_UserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> userService.getUserInfoByUsername("nonexistent"));

        assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
} 