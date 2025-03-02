package com.double2and9.auth_service.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 处理错误页面的控制器
 */
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    /**
     * 处理错误请求
     * 
     * @param request HTTP请求
     * @return 错误页面视图
     */
    @GetMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        // 获取错误状态码
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        ModelAndView modelAndView = new ModelAndView("error/error");
        
        // 默认错误信息
        String errorMessage = "发生未知错误";
        String errorTitle = "错误";
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            // 根据状态码定制错误信息
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorTitle = "页面未找到";
                errorMessage = "请求的页面不存在";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorTitle = "访问被拒绝";
                errorMessage = "您没有权限访问此页面";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorTitle = "服务器错误";
                errorMessage = "服务器内部发生错误";
            }
            
            modelAndView.addObject("statusCode", statusCode);
        }
        
        modelAndView.addObject("errorTitle", errorTitle);
        modelAndView.addObject("errorMessage", errorMessage);
        
        return modelAndView;
    }
} 