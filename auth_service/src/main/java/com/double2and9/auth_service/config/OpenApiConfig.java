package com.double2and9.auth_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI contentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("认证服务 API")
                        .description("提供认证，用户服务，OAuth2和OpenID Connect功能")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("double2and9")
                                .email("micheal0machine@gmail.com")
                                .url("https://github.com/double2and9")))
                .components(new Components()
                        // JWT Bearer认证
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("使用JWT Bearer Token进行认证"))
                        // 客户端认证
                        .addSecuritySchemes("client-authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("使用客户端ID和密钥进行Basic认证"))
                        // OAuth2认证
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth2认证")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl("/api/oauth2/authorize")
                                                .tokenUrl("/api/oauth2/token")
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID Connect认证")
                                                        .addString("profile", "用户基本信息")
                                                        .addString("email", "用户邮箱信息")
                                                        .addString("phone", "用户电话信息")
                                                        .addString("address", "用户地址信息")
                                                        .addString("read", "读取权限")
                                                        .addString("write", "写入权限"))))));
    }
}