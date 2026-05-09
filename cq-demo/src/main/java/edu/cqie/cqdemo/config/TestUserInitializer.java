package edu.cqie.cqdemo.config;

import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Profile("production")
public class TestUserInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public TestUserInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String testUsername = "testuser";
        String testPassword = "test123";
        
        Users existingUser = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Users>()
                .eq(Users::getUsername, testUsername)
                .last("LIMIT 1"));
        
        if (existingUser == null) {
            Users user = new Users();
            user.setUsername(testUsername);
            user.setPassword(passwordEncoder.encode(testPassword));
            user.setEmail("test@example.com");
            user.setRole(0);
            user.setGender(0);
            user.setUserStatus(1);
            
            userMapper.insert(user);
            log.info("测试用户创建成功: {}", testUsername);
        } else {
            log.info("测试用户已存在: {}", testUsername);
        }
    }
}