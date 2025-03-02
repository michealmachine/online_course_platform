package com.double2and9.auth_service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConsentControllerTest {

    @Mock
    private RegisteredClientRepository clientRepository;

    @InjectMocks
    private ConsentController consentController;

    @Test
    public void testShowConsentPage() {
        // 准备测试数据
        String clientId = "test-client";
        String[] scopes = {"read", "profile"};
        String state = "xyz123";
        
        // 创建模拟Principal，不需要设置getName返回值，因为代码中没有使用
        Principal mockPrincipal = mock(Principal.class);
        
        // 模拟客户端存在
        RegisteredClient mockClient = RegisteredClient.withId("1")
                .clientId(clientId)
                .clientName("Test Client")
                // 添加必需的授权类型
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // 添加必需的重定向URI
                .redirectUri("http://localhost:8080/callback")
                .build();
        
        when(clientRepository.findByClientId(clientId)).thenReturn(mockClient);
        
        // 执行测试
        ModelAndView modelAndView = consentController.showConsentPage(clientId, scopes, state, mockPrincipal);
        
        // 验证结果
        assertEquals("auth/consent", modelAndView.getViewName());
        assertTrue(modelAndView.getModel().containsKey("clientId"));
        assertTrue(modelAndView.getModel().containsKey("clientName"));
        assertTrue(modelAndView.getModel().containsKey("scopes"));
        assertTrue(modelAndView.getModel().containsKey("scopeDescriptions"));
        
        assertEquals(clientId, modelAndView.getModel().get("clientId"));
        assertEquals("Test Client", modelAndView.getModel().get("clientName"));
        assertEquals(state, modelAndView.getModel().get("state"));
    }
    
    @Test
    public void testShowConsentPage_InvalidClient() {
        // 创建一个简单的Principal模拟，不需要设置行为
        Principal mockPrincipal = mock(Principal.class);
        
        // 模拟客户端不存在
        when(clientRepository.findByClientId("invalid-client")).thenReturn(null);
        
        // 执行测试并验证异常抛出
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            consentController.showConsentPage("invalid-client", new String[]{"read"}, "state", mockPrincipal);
        });
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("未知的客户端ID"));
    }
    
    @Test
    public void testScopeDescriptions() {
        // 调用获取权限描述的方法
        Map<String, String> scopeDescriptions = consentController.getScopeDescriptions();
        
        // 验证基本权限描述存在
        assertTrue(scopeDescriptions.containsKey("read"));
        assertTrue(scopeDescriptions.containsKey("write"));
        assertTrue(scopeDescriptions.containsKey("openid"));
        
        // 验证关键描述内容
        assertEquals("读取权限", scopeDescriptions.get("read"));
        assertEquals("允许应用读取您的基本信息", scopeDescriptions.get("read.description"));
    }
} 