package edu.cqie.cqdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.LoginDTO;
import edu.cqie.cqdemo.dto.RegisterDTO;
import edu.cqie.cqdemo.entity.User;
import edu.cqie.cqdemo.mapper.UserMapper;
import edu.cqie.cqdemo.service.impl.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录注册控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final edu.cqie.cqdemo.util.JwtUtil jwtUtil;
    private final UserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO loginDTO) {
        try {
            // 1. 认证用户名密码
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );

            // 2. 加载用户信息，生成JWT令牌
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            // 3. 返回令牌
            return Result.success(token);
        } catch (Exception e) {
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 注册接口
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        // 1. 校验用户名是否已存在
        User existUser = sysUserMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (existUser != null) {
            return Result.error("用户名已存在");
        }

        // 2. 密码加密，保存用户
        User sysUser = new User();
        sysUser.setUsername(registerDTO.getUsername());
        sysUser.setPassword(passwordEncoder.encode(registerDTO.getPassword())); // BCrypt加密
        sysUser.setNickname(registerDTO.getNickname() == null ? registerDTO.getUsername() : registerDTO.getNickname());
        sysUserMapper.insert(sysUser);

        return Result.success("注册成功");
    }
}