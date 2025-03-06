package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.RegisterRequest;
import com.double2and9.auth_service.dto.response.CaptchaDTO;
import com.double2and9.auth_service.service.AuthService;
import com.double2and9.auth_service.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 处理注册页面的控制器
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegisterController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /**
     * 显示注册页面
     * @return 注册页面视图
     */
    @GetMapping({"/register", "/register-page"})
    public String showRegisterPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String success,
            Model model
    ) {
        // 处理错误和成功消息
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        
        // 准备注册请求对象
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        
        return "auth/register";
    }
    
    /**
     * 获取验证码
     * @return 验证码DTO
     */
    @GetMapping("/captcha")
    @ResponseBody
    public CaptchaDTO getCaptcha() {
        return captchaService.generateCaptcha();
    }
    
    /**
     * 处理注册表单提交
     */
    @PostMapping("/register")
    public String processRegistration(
            @Valid RegisterRequest registerRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        // 验证表单数据
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/auth/register";
        }
        
        try {
            // 调用注册服务
            authService.register(registerRequest);
            redirectAttributes.addFlashAttribute("success", "注册成功，请登录");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }
} 