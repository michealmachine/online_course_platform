package com.double2and9.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaptchaDTO {
    /**
     * 验证码标识，用于验证
     */
    private String captchaId;
    
    /**
     * 验证码图片（Base64编码）
     */
    private String imageBase64;
} 