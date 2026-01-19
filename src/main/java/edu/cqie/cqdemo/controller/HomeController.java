package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器（测试认证）
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<String> getUserInfo() {
        // 从SecurityContext获取当前登录用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return Result.success("欢迎 " + username + " 登录成功！");
    }
}