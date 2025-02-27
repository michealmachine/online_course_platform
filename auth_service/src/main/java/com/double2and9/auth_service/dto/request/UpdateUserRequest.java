package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UpdateUserRequest {
    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @URL(message = "头像URL格式不正确")
    private String avatar;

    // OIDC相关字段
    @Size(max = 100, message = "名字长度不能超过100个字符")
    private String givenName;
    
    @Size(max = 100, message = "姓氏长度不能超过100个字符")
    private String familyName;
    
    @Size(max = 100, message = "中间名长度不能超过100个字符")
    private String middleName;
    
    @Size(max = 50, message = "首选用户名长度不能超过50个字符")
    private String preferredUsername;
    
    @Pattern(regexp = "^(male|female|other)$", message = "性别只能是 male、female 或 other")
    private String gender;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "出生日期格式必须为 yyyy-MM-dd")
    private String birthdate;
    
    @Size(max = 50, message = "时区长度不能超过50个字符")
    private String zoneinfo;
    
    @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "地区格式必须为 xx-XX")
    private String locale;
} 