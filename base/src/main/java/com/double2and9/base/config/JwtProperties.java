package com.double2and9.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /**
     * JWT密钥
     */
    private String secret;

    /**
     * JWT过期时间（毫秒）
     */
    private long expiration = 86400000; // 默认24小时
} 