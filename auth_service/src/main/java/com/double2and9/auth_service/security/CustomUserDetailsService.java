package com.double2and9.auth_service.security;

import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            // 尝试将 username 转换为 userId
            Long userId = Long.parseLong(username);
            return userRepository.findById(userId)
                    .map(SecurityUser::new)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        } catch (NumberFormatException e) {
            // 如果不是数字，则按用户名查找
            return userRepository.findByUsername(username)
                    .map(SecurityUser::new)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        }
    }
} 