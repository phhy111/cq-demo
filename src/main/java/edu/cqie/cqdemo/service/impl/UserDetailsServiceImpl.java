package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security用户信息加载服务实现类
 * 修改点：
 * 1. 规范依赖注入（lombok构造器注入）
 * 2. 适配用户名不唯一场景（限制查询结果，避免返回多条报错）
 * 3. 增强日志输出，便于问题排查
 * 4. 优化异常提示，补充扩展性（权限集合）
 * 5. 代码规范与注释完善
 */
@Service
@Slf4j
@RequiredArgsConstructor // 替代@Autowired，构造器注入（Spring推荐）
public class UserDetailsServiceImpl implements UserDetailsService {

    // 移除@Autowired，由@RequiredArgsConstructor自动生成构造器注入
    private final UserMapper userMapper;

    /**
     * 根据用户名加载用户信息（适配用户名不唯一场景）
     * @param username 前端传入的登录用户名
     * @return Spring Security标准的UserDetails对象
     * @throws UsernameNotFoundException 用户名不存在/查询异常时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 前置日志：记录登录查询请求
        log.info("开始加载用户信息，查询用户名：{}", username);

        try {
            // 2. 核心修改：适配用户名不唯一场景（加last()限制，避免返回多条结果报错）
            // lambdaQueryWrapper.last("LIMIT 1")：兼容所有数据库；MP的last()是拼接SQL后缀
            Users user = userMapper.selectOne(new LambdaQueryWrapper<Users>()
                    .eq(Users::getUsername, username)
                    .last("LIMIT 1")); // 关键：用户名不唯一时，仅取第一条（或根据业务调整规则）

            // 3. 用户不存在校验
            if (user == null) {
                log.warn("用户名不存在，登录失败：{}", username);
                throw new UsernameNotFoundException("登录失败：用户名不存在"); // 友好提示，隐藏敏感信息
            }

            // 4. 用户存在日志（脱敏密码，避免日志泄露）
            log.info("成功加载用户信息，用户名：{}，用户ID：{}", username, user.getId());

            // 5. 构建UserDetails（补充权限集合扩展，当前为空列表，后续可扩展）
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),          // 用户名
                    user.getPassword(),          // 加密后的密码（BCrypt）
                    true,                        // 账户是否启用（默认启用）
                    true,                        // 账户是否过期
                    true,                        // 凭证是否过期
                    true,                        // 账户是否锁定
                    java.util.Collections.emptyList() // 权限集合（后续可从数据库查询角色/权限）
            );
        } catch (Exception e) {
            // 6. 异常兜底处理：捕获查询异常，转为UsernameNotFoundException
            log.error("加载用户信息失败，用户名：{}，异常原因：{}", username, e.getMessage(), e);
            throw new UsernameNotFoundException("登录失败：用户信息查询异常", e);
        }
    }
}