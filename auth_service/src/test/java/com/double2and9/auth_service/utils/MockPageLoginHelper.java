package com.double2and9.auth_service.utils;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用于在测试中模拟页面登录的工具类
 */
public class MockPageLoginHelper {

    /**
     * 执行模拟页面表单登录
     * 
     * @param mockMvc MockMvc实例
     * @param username 用户名
     * @param password 密码
     * @param session 当前会话
     * @param rememberMe 是否记住我
     * @return 登录请求的结果
     * @throws Exception 如果执行过程中发生异常
     */
    public static MvcResult performFormLogin(
            MockMvc mockMvc,
            String username,
            String password,
            MockHttpSession session,
            boolean rememberMe) throws Exception {
        
        return mockMvc.perform(post("/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", username)
                .param("password", password)
                .param("remember-me", String.valueOf(rememberMe)))
                .andReturn();
    }
    
    /**
     * 执行模拟页面表单登录（默认不记住我）
     */
    public static MvcResult performFormLogin(
            MockMvc mockMvc,
            String username,
            String password,
            MockHttpSession session) throws Exception {
        return performFormLogin(mockMvc, username, password, session, false);
    }
    
    /**
     * 执行模拟页面表单登录，并检查重定向状态
     */
    public static MvcResult performFormLoginAndExpectRedirect(
            MockMvc mockMvc,
            String username,
            String password,
            MockHttpSession session) throws Exception {
        
        return mockMvc.perform(post("/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", username)
                .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
    }
    
    /**
     * 执行模拟页面表单登录，带OAuth2参数
     */
    public static MvcResult performOAuth2FormLogin(
            MockMvc mockMvc,
            String username,
            String password,
            MockHttpSession session,
            String clientId,
            String redirectUri,
            String state,
            String codeChallenge,
            String codeChallengeMethod) throws Exception {
        
        return mockMvc.perform(post("/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", username)
                .param("password", password)
                .param("client_id", clientId)
                .param("redirect_uri", redirectUri)
                .param("state", state)
                .param("code_challenge", codeChallenge)
                .param("code_challenge_method", codeChallengeMethod))
                .andReturn();
    }
} 