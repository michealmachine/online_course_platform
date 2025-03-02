package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;

/**
 * 处理登录界面的控制器
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthService authService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    /**
     * 显示登录页面
     * @return 登录页面视图
     */
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String success,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("error", "登录失败：" + error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }
    
    /**
     * 显示注册页面
     * @return 注册页面视图
     */
    @GetMapping("/register")
    public String showRegisterPage() {
        // 先返回登录页面，后续实现注册页面
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(
            @Valid LoginRequest loginRequest,
            BindingResult bindingResult,
            @RequestParam(name = "remember-me", required = false) boolean rememberMe,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        // 验证表单数据
        if (bindingResult.hasErrors()) {
            redirectAttributes.addAttribute("error", "请输入有效的用户名和密码");
            return "redirect:/auth/login";
        }

        try {
            // 获取客户端IP地址
            String clientIp = getClientIp(request);
            
            // 调用AuthService进行身份验证，传递IP地址
            AuthResponse authResponse = authService.login(loginRequest, clientIp);
            
            // 如果登录成功，设置JWT令牌到Cookie
            if (authResponse != null && authResponse.getToken() != null) {
                setCookieToken(response, authResponse.getToken(), rememberMe);
                
                // 登录成功后，重定向到原始请求URL或默认首页
                SavedRequest savedRequest = requestCache.getRequest(request, response);
                if (savedRequest != null) {
                    String redirectUrl = savedRequest.getRedirectUrl();
                    requestCache.removeRequest(request, response);
                    response.sendRedirect(redirectUrl);
                    return null;
                } else {
                    return "redirect:/";
                }
            } else {
                // 登录失败
                redirectAttributes.addAttribute("error", "认证失败，请检查用户名和密码");
                return "redirect:/auth/login";
            }
        } catch (Exception e) {
            // 处理登录异常
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/auth/login";
        }
    }

    /**
     * 设置JWT令牌到Cookie
     */
    private void setCookieToken(HttpServletResponse response, String token, boolean rememberMe) {
        Cookie cookie = new Cookie("auth_token", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        
        // 如果选择了"记住我"功能，设置较长的过期时间
        if (rememberMe) {
            // 7天 (7 * 24 * 60 * 60)
            cookie.setMaxAge(604800);
        } else {
            // 2小时 (2 * 60 * 60)
            cookie.setMaxAge(7200);
        }
        
        response.addCookie(cookie);
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
} 