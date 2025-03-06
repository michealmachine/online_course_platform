package com.double2and9.auth_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.*;

/**
 * OAuth2授权确认控制器
 * 处理授权确认页面的展示
 */
@Controller
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2ConsentController {

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationConsentService authorizationConsentService;

    /**
     * 显示授权确认页面
     * 当用户需要确认应用的访问权限时调用此方法
     */
    @GetMapping("/consent")
    public String consent(
            Principal principal,
            Model model,
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
            @RequestParam(OAuth2ParameterNames.STATE) String state,
            @RequestParam(value = OAuth2ParameterNames.USER_CODE, required = false) String userCode) {

        // 1. 查找客户端
        RegisteredClient client = this.registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalArgumentException("客户端不存在");
        }

        // 2. 准备授权范围
        Set<String> scopesToApprove = new HashSet<>();
        Set<String> previouslyApprovedScopes = new HashSet<>();

        // 3. 处理授权范围
        Set<String> requestedScopes = StringUtils.commaDelimitedListToSet(scope);
        OAuth2AuthorizationConsent currentConsent = this.authorizationConsentService.findById(
                client.getId(), principal.getName());

        // 4. 判断授权状态
        if (currentConsent != null) {
            previouslyApprovedScopes = currentConsent.getScopes();
        }

        // 5. 过滤需要用户确认的授权范围
        for (String requestedScope : requestedScopes) {
            if (!previouslyApprovedScopes.contains(requestedScope)) {
                scopesToApprove.add(requestedScope);
            }
        }

        // 6. 如果没有需要新批准的范围，则直接返回
        if (scopesToApprove.isEmpty()) {
            return "redirect:/oauth2/authorize?client_id=" + clientId +
                   "&scope=" + scope +
                   "&state=" + state +
                   (userCode != null ? "&user_code=" + userCode : "");
        }

        // 7. 添加模型属性
        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", client.getClientName());
        model.addAttribute("state", state);
        model.addAttribute("scopes", scopesToApprove);
        model.addAttribute("previouslyApprovedScopes", previouslyApprovedScopes);
        model.addAttribute("userCode", userCode);
        
        // 8. 准备范围描述
        Map<String, String> scopeDescriptions = new HashMap<>();
        scopeDescriptions.put(OidcScopes.PROFILE, "访问您的个人资料信息");
        scopeDescriptions.put(OidcScopes.EMAIL, "访问您的电子邮件地址");
        scopeDescriptions.put("message.read", "读取您的消息");
        scopeDescriptions.put("message.write", "发送和管理消息");
        scopeDescriptions.put("admin", "管理员访问权限");
        model.addAttribute("scopeDescriptions", scopeDescriptions);

        return "auth/consent";
    }
} 