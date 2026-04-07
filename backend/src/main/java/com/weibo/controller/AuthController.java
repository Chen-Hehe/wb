package com.weibo.controller;

import com.weibo.common.Result;
import com.weibo.dto.LoginDTO;
import com.weibo.dto.RegisterDTO;
import com.weibo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return Result.success("注册成功", userService.register(registerDTO));
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        return Result.success("登录成功", token);
    }
    
    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public Result<?> refreshToken(@RequestParam String refreshToken) {
        String token = userService.refreshToken(refreshToken);
        return Result.success("刷新成功", token);
    }
}
