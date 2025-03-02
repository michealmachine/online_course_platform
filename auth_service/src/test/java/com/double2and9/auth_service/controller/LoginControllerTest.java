package com.double2and9.auth_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoginControllerTest {

    private final LoginController loginController = new LoginController();

    @Test
    public void testShowLoginPage() {
        ModelAndView modelAndView = loginController.showLoginPage();
        assertEquals("auth/login", modelAndView.getViewName());
    }

    @Test
    public void testShowRegisterPage() {
        ModelAndView modelAndView = loginController.showRegisterPage();
        assertEquals("auth/login", modelAndView.getViewName());
    }
} 