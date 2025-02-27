package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.Set;

@Data
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度必须在4-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和连字符")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotEmpty(message = "角色不能为空")
    private Set<String> roles;  // 可以指定多个角色
    
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;
    
    @Pattern(regexp = "^[0-9]{11}$", message = "手机号格式不正确")
    private String phone;
    
    // 机构用户字段
    private Long organizationId;
    
    // OIDC相关字段
    @Size(max = 100, message = "名字长度不能超过100个字符")
    private String givenName;
    
    @Size(max = 100, message = "姓氏长度不能超过100个字符")
    private String familyName;
    
    @Size(max = 100, message = "中间名长度不能超过100个字符")
    private String middleName;
    
    @Size(max = 50, message = "首选用户名长度不能超过50个字符")
    private String preferredUsername;
    
    @URL(message = "个人资料链接必须是有效的URL")
    @Size(max = 255, message = "个人资料链接长度不能超过255个字符")
    private String profile;
    
    @URL(message = "网站链接必须是有效的URL")
    @Size(max = 255, message = "网站链接长度不能超过255个字符")
    private String website;
    
    @Pattern(regexp = "^(male|female|other)$", message = "性别只能是 male、female 或 other")
    private String gender;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "出生日期格式必须为 yyyy-MM-dd")
    private String birthdate;
    
    @Size(max = 50, message = "时区长度不能超过50个字符")
    private String zoneinfo;
    
    @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "地区格式必须为 xx-XX")
    private String locale;
    // ... 其他字段 ...
} 