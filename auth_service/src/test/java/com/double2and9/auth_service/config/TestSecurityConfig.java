package com.double2and9.auth_service.config;

import com.double2and9.base.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试环境专用的安全配置
 */
@TestConfiguration
public class TestSecurityConfig {

    // 设置与开发环境相同的JWT密钥
    public static final String DEV_SECRET = "ZDY1NmE3ZjI0ODdiNGZkZWJmZjk5MjExYjU4MzRlMzE2NzIwZWM0MjJmNmFlOWY5NzM2ZmRkMGM5NTYzYzE3YQ";
    private static final long TEST_EXPIRATION = 3600L; // 1小时
    
    // 测试JWT配置，延迟初始化
    private static JwtProperties jwtProperties;
    private static Key signingKey;

    @Bean
    @Primary
    public AuthorizationServerSettings testAuthorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://localhost:8084")
            .build();
    }
    
    /**
     * 测试环境使用的密码编码器，不进行任何加密
     * 注意：仅用于测试环境，生产环境必须使用安全的密码编码器
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
    
    /**
     * 测试环境使用的认证提供器，配合不加密的密码编码器使用
     * 注意：仅用于测试环境
     */
    @Bean
    @Primary
    public AuthenticationProvider testAuthenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(testPasswordEncoder());
        return provider;
    }
    
    /**
     * 为测试环境提供JWT配置属性
     * 使用与开发环境相同的密钥确保一致性
     */
    @Bean
    @Primary
    public JwtProperties testJwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(DEV_SECRET);
        properties.setExpiration(86400000L); // 24小时
        jwtProperties = properties; // 保存实例用于令牌生成
        return properties;
    }
    
    /**
     * 获取签名密钥
     */
    private static Key getSigningKey() {
        if (signingKey == null) {
            try {
                byte[] keyBytes = Decoders.BASE64URL.decode(DEV_SECRET);
                signingKey = Keys.hmacShaKeyFor(keyBytes);
            } catch (Exception e) {
                // 如果密钥解析失败，生成一个安全的密钥
                signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            }
        }
        return signingKey;
    }
    
    /**
     * 为测试生成有效的JWT令牌
     * @param subject 用户标识
     * @return JWT令牌字符串
     */
    public static String generateTestJwtToken(String subject) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", subject);
            claims.put("roles", "ROLE_USER");
            claims.put("clientId", "web-client");
            claims.put("username", subject);
            claims.put("userId", "1");
            claims.put("scope", "openid profile email");
            claims.put("type", "access_token");
            
            Date now = new Date();
            Date expiration = new Date(now.getTime() + TEST_EXPIRATION * 1000);
            
            // 确保JWT签名与应用相同 - 输出详细调试信息
            System.out.println("测试令牌生成 - 使用密钥: " + DEV_SECRET.substring(0, 10) + "...");
            Key key = getSigningKey();
            System.out.println("测试令牌生成 - 签名算法: HS512");
            
            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .compact();
        } catch (Exception e) {
            System.err.println("生成测试JWT令牌失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 为测试生成有效的ID令牌
     * @param subject 用户标识
     * @return ID令牌字符串
     */
    public static String generateTestIdToken(String subject) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", subject);
            claims.put("name", "Test User");
            claims.put("email", "test@example.com");
            claims.put("nonce", "test-nonce");
            claims.put("clientId", "web-client");
            claims.put("userId", "1");
            claims.put("username", subject);
            claims.put("type", "id_token");
            claims.put("iss", "http://localhost:8084");
            claims.put("aud", "web-client");
            
            Date now = new Date();
            Date expiration = new Date(now.getTime() + TEST_EXPIRATION * 1000);
            
            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                    .compact();
        } catch (Exception e) {
            System.err.println("生成测试ID令牌失败: " + e.getMessage());
            throw e;
        }
    }
} 