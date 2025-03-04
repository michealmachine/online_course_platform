package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.CaptchaDTO;

/**
 * 验证码服务接口
 */
public interface CaptchaService {
    
    /**
     * 生成验证码
     * @return 验证码DTO，包含验证码ID和图片Base64
     */
    CaptchaDTO generateCaptcha();
    
    /**
     * 验证验证码
     * @param captchaId 验证码ID
     * @param userInput 用户输入的验证码
     * @return 验证结果，true表示验证通过，false表示验证失败
     */
    boolean validateCaptcha(String captchaId, String userInput);
} 