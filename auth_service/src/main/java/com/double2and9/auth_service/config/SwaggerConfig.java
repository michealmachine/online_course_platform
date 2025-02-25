package com.double2and9.auth_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "认证服务 API",
        description = "认证服务接口文档",
        version = "1.0"
    )
)
@SecuritySchemes({
    @SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "使用JWT令牌进行认证"
    ),
    @SecurityScheme(
        name = "client-authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "使用客户端ID和密钥进行HTTP Basic认证"
    )
})
public class SwaggerConfig {
    // 配置类可以为空，注解已经完成了必要的配置
}