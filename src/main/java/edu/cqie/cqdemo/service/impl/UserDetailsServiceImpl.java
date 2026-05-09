package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.cqie.cqdemo.entity.LoginUser; // 引入自定义LoginUser
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("开始加载用户信息，查询用户名：{}", username);

        try {
            Users user = userMapper.selectOne(new LambdaQueryWrapper<Users>()
                    .eq(Users::getUsername, username)
                    .last("LIMIT 1"));

            if (user == null) {
                log.warn("用户名不存在，登录失败：{}", username);
                throw new UsernameNotFoundException("登录失败：用户名不存在");
            }

            log.info("成功加载用户信息，用户名：{}，用户ID：{}", username, user.getId());

            // 直接返回自定义LoginUser，无构造器报错，且携带UserID
            return new LoginUser(user);
        } catch (Exception e) {
            log.error("加载用户信息失败，用户名：{}，异常原因：{}", username, e.getMessage(), e);
            throw new UsernameNotFoundException("登录失败：用户信息查询异常", e);
        }
    }
}