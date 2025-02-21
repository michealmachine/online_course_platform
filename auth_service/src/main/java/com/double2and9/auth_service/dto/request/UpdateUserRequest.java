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
} 