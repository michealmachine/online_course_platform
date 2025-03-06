package com.double2and9.auth_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2测试控制器
 * 用于测试OAuth2标准端点
 */
@Slf4j
@Tag(name = "OAuth2测试", description = "测试OAuth2/OIDC流程")
@RestController
@RequestMapping("/api/oauth2-test")
@RequiredArgsConstructor
public class OAuth2TestController {

    /**
     * 测试OAuth2授权请求
     * 创建一个指向OAuth2授权端点的链接
     */
    @Operation(summary = "测试OAuth2授权请求")
    @GetMapping("/authorize-link")
    public ModelAndView getAuthorizeLink() {
        ModelAndView mv = new ModelAndView("oauth2/authorize-test");
        
        // 构建授权URL
        String authorizeUrl = "/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=web-client" +
                "&redirect_uri=http://localhost:3000/callback" +
                "&scope=openid+profile+email" +
                "&state=test-state-" + System.currentTimeMillis();
                
        mv.addObject("authorizeUrl", authorizeUrl);
        return mv;
    }
    
    /**
     * 模拟OAuth2回调请求
     * 用于测试授权码流程的回调处理
     */
    @Operation(summary = "模拟OAuth2回调处理")
    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(
            @RequestParam(value = OAuth2ParameterNames.CODE, required = false) String code,
            @RequestParam(value = OAuth2ParameterNames.STATE, required = false) String state,
            @RequestParam(value = OAuth2ParameterNames.ERROR, required = false) String error) {
            
        Map<String, String> response = new HashMap<>();
        
        if (error != null) {
            response.put("status", "error");
            response.put("error", error);
            return ResponseEntity.badRequest().body(response);
        }
        
        response.put("status", "success");
        response.put("code", code);
        response.put("state", state);
        
        // 记录授权成功信息
        log.info("授权码流程成功完成，授权码：{}，状态：{}", code, state);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取当前认证信息
     * 用于验证OAuth2/JWT认证是否正常工作
     */
    @Operation(summary = "获取当前认证信息")
    @GetMapping("/auth-info")
    public ResponseEntity<Map<String, Object>> getAuthInfo() {
        Map<String, Object> info = new HashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            info.put("authenticated", auth.isAuthenticated());
            info.put("principal", auth.getPrincipal());
            info.put("name", auth.getName());
            info.put("authorities", auth.getAuthorities());
            info.put("details", auth.getDetails());
            info.put("authenticationType", auth.getClass().getName());
        } else {
            info.put("authenticated", false);
            info.put("message", "No authentication present");
        }
        
        return ResponseEntity.ok(info);
    }
} 