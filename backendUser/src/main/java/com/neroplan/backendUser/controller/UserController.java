package com.neroplan.backendUser.controller;

import com.neroplan.backendUser.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor //final 필드 생성자 자동 생성 어노테이션
@RequestMapping("/users")
public class UserController {
    final UserService   userService;

    @RequestMapping("/googlelogin")
    public String googleLogin()
    {
        this.userService.googleLogin();
        return "Login";
    }
}
