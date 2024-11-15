package com.double2and9.content_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI contentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("内容管理服务 API")
                        .description("提供课程管理、课程计划管理、教师管理等功能")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("double2and9")
                                .email("micheal0machine@gmail.com")
                                .url("https://github.com/double2and9")));
    }
} 