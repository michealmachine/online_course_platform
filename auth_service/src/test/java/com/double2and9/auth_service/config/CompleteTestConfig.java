package com.double2and9.auth_service.config;

import com.double2and9.auth_service.dto.response.ResourceMeta;
import com.double2and9.auth_service.repository.CustomJdbcRegisteredClientRepository;
import com.double2and9.auth_service.security.CustomUserDetailsService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * 提供完整测试环境所需的所有依赖
 */
@TestConfiguration
@Import(TestSecurityConfig.class)
public class CompleteTestConfig {

    /**
     * 提供模拟的UserDetailsService
     */
    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    /**
     * 提供模拟的RegisteredClientRepository
     */
    @Bean
    @Primary
    public RegisteredClientRepository registeredClientRepository() {
        return Mockito.mock(RegisteredClientRepository.class);
    }

    /**
     * 提供模拟的CustomJdbcRegisteredClientRepository
     */
    @Bean
    @Primary
    public CustomJdbcRegisteredClientRepository customJdbcRegisteredClientRepository() {
        return Mockito.mock(CustomJdbcRegisteredClientRepository.class);
    }

    /**
     * 提供模拟的OAuth2AuthorizationService
     */
    @Bean
    @Primary
    public OAuth2AuthorizationService oAuth2AuthorizationService() {
        return Mockito.mock(OAuth2AuthorizationService.class);
    }

    /**
     * 提供模拟的OAuth2AuthorizationConsentService
     */
    @Bean
    @Primary
    public OAuth2AuthorizationConsentService oAuth2AuthorizationConsentService() {
        return Mockito.mock(OAuth2AuthorizationConsentService.class);
    }

    /**
     * 提供模拟的JdbcTemplate
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate() {
        return Mockito.mock(JdbcTemplate.class);
    }

    /**
     * 提供模拟的RedisConnectionFactory
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    /**
     * 提供模拟的RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(template.opsForValue()).thenReturn(valueOps);
        return template;
    }

    /**
     * 提供模拟的RedisCache
     */
    @Bean
    @Primary
    public RedisCache redisCache() {
        return Mockito.mock(RedisCache.class);
    }

    /**
     * 提供ResourceMeta的测试实现
     */
    @Bean
    @Primary
    public Map<String, ResourceMeta> resourceMetaMap() {
        Map<String, ResourceMeta> map = new HashMap<>();
        
        map.put("user", new ResourceMeta("用户管理", "用户管理", 1, 1));
        map.put("role", new ResourceMeta("角色管理", "角色管理", 2, 2));
        map.put("permission", new ResourceMeta("权限管理", "权限管理", 3, 3));
        
        return map;
    }
} 