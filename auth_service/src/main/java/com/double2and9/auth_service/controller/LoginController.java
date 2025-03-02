package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.dto.request.OAuth2AuthorizationRequest;
import com.double2and9.auth_service.dto.response.AuthResponse;
import com.double2and9.auth_service.service.AuthService;
import com.double2and9.auth_service.service.AuthorizationRequestService;
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
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * 处理登录界面的控制器
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthService authService;
    private final AuthorizationRequestService authorizationRequestService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    /**
     * 显示登录页面
     * @return 登录页面视图
     */
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String success,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        // 处理错误和成功消息
        if (error != null) {
            model.addAttribute("error", "登录失败：" + error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        
        // 准备登录请求对象
        model.addAttribute("loginRequest", new LoginRequest());
        
        // 检查是否是从OAuth2授权请求重定向过来的
        OAuth2AuthorizationRequest authorizationRequest = 
                authorizationRequestService.extractAuthorizationRequest(request);
        
        if (authorizationRequest != null) {
            // 保存授权请求参数到会话，以便登录成功后恢复授权流程
            authorizationRequestService.saveAuthorizationRequest(authorizationRequest, request, response);
            model.addAttribute("continueAuthorization", true);
        }
        
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
                
                // 检查是否有保存的OAuth2授权请求
                OAuth2AuthorizationRequest authorizationRequest = 
                        authorizationRequestService.getAuthorizationRequest(request, response);
                
                if (authorizationRequest != null && authorizationRequest.isContinueAuthorization()) {
                    // 移除已处理的授权请求
                    authorizationRequestService.removeAuthorizationRequest(request, response);
                    
                    // 重定向回OAuth2授权端点，继续授权流程
                    String redirectUrl = authorizationRequest.buildAuthorizationRequestUrl();
                    response.sendRedirect(redirectUrl);
                    return null;
                }
                
                // 如果没有OAuth2授权请求，使用标准的请求缓存机制
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