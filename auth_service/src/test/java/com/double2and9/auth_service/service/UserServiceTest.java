package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.UpdateUserRequest;
import com.double2and9.auth_service.dto.response.UserResponse;
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

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        updateRequest = new UpdateUserRequest();
        updateRequest.setNickname("新昵称");
        updateRequest.setPhone("13800138000");
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
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

        UserResponse result = userService.updateUser(1L, updateRequest);

        assertNotNull(result);
        assertEquals("新昵称", testUser.getNickname());
        assertEquals("13800138000", testUser.getPhone());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(userMapper).toUserResponse(testUser);
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
} 