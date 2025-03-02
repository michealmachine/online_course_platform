package com.double2and9.auth_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 处理登录界面的控制器
 */
@Controller
@RequestMapping("/auth")
public class LoginController {

    /**
     * 显示登录页面
     * @return 登录页面视图
     */
    @GetMapping("/login")
    public ModelAndView showLoginPage() {
        return new ModelAndView("auth/login");
    }
    
    /**
     * 显示注册页面
     * @return 注册页面视图
     */
    @GetMapping("/register")
    public ModelAndView showRegisterPage() {
        // 目前只返回登录页面，后续实现注册功能
        return new ModelAndView("auth/login");
    }
} 