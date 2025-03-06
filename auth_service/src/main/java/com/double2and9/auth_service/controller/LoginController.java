package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.LoginRequest;
import com.double2and9.auth_service.service.AuthService;
import com.double2and9.auth_service.service.AuthorizationRequestService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * 登录控制器
 * 处理用户登录相关功能
 */
@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {
    
    private final AuthService authService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    /**
     * 显示登录页面
     */
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String success,
            HttpServletRequest request,
            Model model) {
        
        // 添加登录请求表单对象
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        
        // 添加错误或成功信息
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }
        
        // 获取授权请求的继续标记
        SavedRequest savedRequest = requestCache.getRequest(request, null);
        if (savedRequest != null && savedRequest.getRedirectUrl().contains("/oauth2/authorize")) {
            model.addAttribute("continueAuthorization", true);
        }
        
        return "auth/login";
    }

    /**
     * 处理登录表单提交
     * 注意：表单登录已由Spring Security处理，此方法是为了处理特殊场景下的表单提交
     */
    @PostMapping("/login")
    public String processLogin(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult bindingResult,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String response_type,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code_challenge,
            @RequestParam(required = false) String code_challenge_method,
            @RequestParam(required = false) String nonce,
            @RequestParam(required = false) String continue_authorization,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // 验证表单输入
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        
        try {
            // 1. 调用登录服务进行认证
            String token = authService.login(loginRequest.getUsername(), loginRequest.getPassword(), getClientIp(request));
            
            // 2. 设置JWT令牌到Cookie
            setCookieToken(response, token, Optional.ofNullable(request.getParameter("remember-me"))
                    .map(Boolean::parseBoolean)
                    .orElse(false));
            
            // 3. 处理OAuth2授权请求
            if (Boolean.parseBoolean(continue_authorization)) {
                // 构建OAuth2授权请求重定向URL
                StringBuilder redirectUrl = new StringBuilder("/oauth2/authorize");
                boolean hasParam = false;
                
                if (client_id != null) {
                    redirectUrl.append("?client_id=").append(client_id);
                    hasParam = true;
                }
                if (redirect_uri != null) {
                    redirectUrl.append(hasParam ? "&" : "?").append("redirect_uri=").append(redirect_uri);
                    hasParam = true;
                }
                if (response_type != null) {
                    redirectUrl.append(hasParam ? "&" : "?").append("response_type=").append(response_type);
                    hasParam = true;
                }
                if (scope != null) {
                    redirectUrl.append(hasParam ? "&" : "?").append("scope=").append(scope);
                    hasParam = true;
                }
                if (state != null) {
                    redirectUrl.append(hasParam ? "&" : "?").append("state=").append(state);
                    hasParam = true;
                }
                if (code_challenge != null) {
                    redirectUrl.append(hasParam ? "&" : "?").append("code_challenge=").append(code_challenge);
                    hasParam = true;
                }
                if (code_challenge_method != null) {
                    redirectUrl.append(hasParam ? "&" : "?").append("code_challenge_method=").append(code_challenge_method);
                    hasParam = true;
                }
                if (nonce != null) {
                    redirectUrl.append(hasParam ? "&" : "?").append("nonce=").append(nonce);
                }
                
                return "redirect:" + redirectUrl;
            }
            
            // 4. 处理常规登录请求的重定向
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (savedRequest != null) {
                String targetUrl = savedRequest.getRedirectUrl();
                requestCache.removeRequest(request, response);
                return "redirect:" + targetUrl;
            }
            
            return "redirect:/";
            
        } catch (Exception e) {
            log.error("登录失败", e);
            redirectAttributes.addFlashAttribute("error", "用户名或密码错误");
            return "redirect:/auth/login";
        }
    }
    
    /**
     * 设置JWT令牌到Cookie
     */
    private void setCookieToken(HttpServletResponse response, String token, boolean rememberMe) {
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        
        // 如果是"记住我"，则设置较长的过期时间
        if (rememberMe) {
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7天
        } else {
            cookie.setMaxAge(-1); // 会话Cookie
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