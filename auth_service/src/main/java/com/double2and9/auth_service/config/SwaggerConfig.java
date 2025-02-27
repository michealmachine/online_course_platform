package com.double2and9.auth_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "认证服务 API",
        description = """
            认证服务接口文档
            
            ## 支持的认证流程
            - 授权码模式（Authorization Code Flow）
            - 刷新令牌（Refresh Token）
            
            ## OpenID Connect 功能
            - ID Token 生成和验证
            - UserInfo 端点
            - Token 内省
            - PKCE 支持
            """,
        version = "1.0",
        contact = @Contact(
            name = "Double2and9",
            email = "support@double2and9.com",
            url = "https://www.double2and9.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
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
public class SwaggerConfig {}