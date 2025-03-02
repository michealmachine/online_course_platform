package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.OAuth2AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 授权请求存储服务接口
 * 用于在登录过程中保存和恢复OAuth2授权请求参数
 */
public interface AuthorizationRequestService {

    /**
     * 保存授权请求参数
     * @param authorizationRequest 授权请求参数
     * @param request HTTP请求
     * @param response HTTP响应
     */
    void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, 
                                HttpServletRequest request, 
                                HttpServletResponse response);

    /**
     * 获取保存的授权请求参数
     * @param request HTTP请求
     * @param response HTTP响应
     * @return 保存的授权请求参数，如果不存在则返回null
     */
    OAuth2AuthorizationRequest getAuthorizationRequest(HttpServletRequest request, 
                                                    HttpServletResponse response);

    /**
     * 移除保存的授权请求参数
     * @param request HTTP请求
     * @param response HTTP响应
     */
    void removeAuthorizationRequest(HttpServletRequest request, 
                                  HttpServletResponse response);
    
    /**
     * 从HTTP请求中提取授权请求参数
     * @param request HTTP请求
     * @return 提取的授权请求参数
     */
    OAuth2AuthorizationRequest extractAuthorizationRequest(HttpServletRequest request);
} 