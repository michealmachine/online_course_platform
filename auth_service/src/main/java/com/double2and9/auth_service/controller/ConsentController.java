package com.double2and9.auth_service.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import lombok.RequiredArgsConstructor;

/**
 * 处理授权确认界面的控制器
 */
@Controller
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class ConsentController {

    private final RegisteredClientRepository clientRepository;
    
    /**
     * 显示授权确认页面
     * 
     * @param clientId 客户端ID
     * @param scopes 请求的权限范围
     * @param state OAuth2状态参数
     * @param principal 当前认证用户
     * @return 授权确认页面视图
     */
    @GetMapping("/consent")
    public ModelAndView showConsentPage(
            @RequestParam("client_id") String clientId,
            @RequestParam("scope") String[] scopes,
            @RequestParam(value = "state", required = false) String state,
            Principal principal) {
        
        // 获取客户端信息
        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalArgumentException("未知的客户端ID: " + clientId);
        }
        
        // 构建权限范围描述
        Map<String, String> scopeDescriptions = getScopeDescriptions();
        
        // 准备视图模型
        ModelAndView modelAndView = new ModelAndView("auth/consent");
        modelAndView.addObject("clientId", clientId);
        modelAndView.addObject("clientName", client.getClientName());
        modelAndView.addObject("state", state);
        modelAndView.addObject("scopes", scopes);
        modelAndView.addObject("scopeDescriptions", scopeDescriptions);
        modelAndView.addObject("principal", principal);
        
        return modelAndView;
    }
    
    /**
     * 获取权限范围的描述
     * 
     * @return 权限范围及其描述的映射
     */
    public Map<String, String> getScopeDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        
        // 基本权限
        descriptions.put("read", "读取权限");
        descriptions.put("read.description", "允许应用读取您的基本信息");
        
        descriptions.put("write", "写入权限");
        descriptions.put("write.description", "允许应用更新您的信息");
        
        // OIDC相关权限
        descriptions.put("openid", "身份验证");
        descriptions.put("openid.description", "允许应用验证您的身份");
        
        descriptions.put("profile", "个人资料");
        descriptions.put("profile.description", "允许应用访问您的个人基本资料");
        
        descriptions.put("email", "电子邮件");
        descriptions.put("email.description", "允许应用访问您的电子邮件地址");
        
        descriptions.put("phone", "电话号码");
        descriptions.put("phone.description", "允许应用访问您的电话号码");
        
        descriptions.put("address", "地址信息");
        descriptions.put("address.description", "允许应用访问您的地址信息");
        
        descriptions.put("mobile", "移动设备访问");
        descriptions.put("mobile.description", "允许从移动设备访问您的账户");
        
        return descriptions;
    }
} 