package com.neroplan.backendUser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/auth")
public class OAuthController {

    @GetMapping("/login/google")
    public String googleLogin() {

        // Spring Security가 제공하는 OAuth2 로그인 시작 URL
        return "redirect:/oauth2/authorization/google";
    }
}