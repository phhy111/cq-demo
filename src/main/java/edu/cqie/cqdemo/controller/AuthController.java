package edu.cqie.cqdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.LoginDTO;
import edu.cqie.cqdemo.dto.RegisterDTO;

import edu.cqie.cqdemo.entity.Users;

import edu.cqie.cqdemo.mapper.UserMapper;
import edu.cqie.cqdemo.service.impl.UserDetailsServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jakarta.mail.internet.MimeMessage;

/**
 * 登录注册控制器（最终优化版）
 * 修改点：
 * 1. 移除用户名唯一性校验，仅校验邮箱+手机号唯一性
 * 2. 清理重复的依赖注入（@RequiredArgsConstructor + @Autowired 二选一）
 * 3. 移除@RequestBody，适配FormData接收文件
 * 4. 处理字段类型转换异常（ext2/ext4）
 * 5. 优化异常提示，增强代码健壮性
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // 统一用lombok注入，移除@Autowired
@Slf4j
@Validated
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final edu.cqie.cqdemo.util.JwtUtil jwtUtil;
    private final UserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender mailSender;


    /**
     * 登录接口（保持不变，登录仍用JSON接收）
     */
    @PostMapping("/login")
    public Result<java.util.Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            // 登录信息写入 Redis，保证 2 小时时效
            String loginKey = "login_token:" + userDetails.getUsername();
            stringRedisTemplate.opsForValue().set(loginKey, token, 2, TimeUnit.HOURS);

            // 根据用户名查询用户信息
            Users user = sysUserMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Users>()
                    .eq(Users::getUsername, loginDTO.getUsername()));
            System.out.println("当前登录信息"+user);

            // 构建返回结果
            java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
            resultMap.put("token", token);
            resultMap.put("user", user);


            return Result.success(resultMap);
        } catch (Exception e) {
            log.error("登录失败：", e);
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 发送注册验证码接口
     * 验证码写入 Redis，失效时间 10 分钟
     */
    @PostMapping("/sendCode")
    public Result<String> sendVerifyCode(@RequestBody RegisterDTO dto) {
        // 生成 6 位随机验证码
        String code = String.format("%06d", (int) (Math.random() * 900000) + 100000);

        String redisKey = "verify_code:" + dto.getEmail();
        // 验证码写入 Redis，10 分钟过期
        stringRedisTemplate.opsForValue().set(redisKey, code, 10, TimeUnit.MINUTES);

        // 通过QQ邮箱发送验证码邮件
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("2686262955@qq.com");
            helper.setTo(dto.getEmail());
            helper.setSubject("注册验证码");
            helper.setText("行千里，致广大！欢迎使用～～智伴出行，行达天下，愿每一段旅程都轻松美好～" +
                    "您的验证码为：\n"+ code+"，10 分钟内有效，请尽快完成注册。\n", false);
            mailSender.send(message);
            log.info("注册验证码邮件发送成功，email: {}, code: {}", dto.getEmail(), code);
        } catch (Exception e) {
            log.error("发送验证码邮件失败，email: {}", dto.getEmail(), e);
            return Result.error("验证码发送失败，请稍后重试");
        }

        return Result.success("验证码已发送，请在10分钟内完成注册");
    }


    /**
     * 注册接口（核心优化）
     * 移除@RequestBody：适配FormData接收文件（MultipartFile）
     */
    @PostMapping("/register")
    public Result<String> register(@Valid RegisterDTO registerDTO) {
        try {
            // ========== 2. 核心修改：移除用户名唯一性校验，仅校验邮箱+手机号 ==========
            // 2.1 校验邮箱唯一性（必填项）
            Users existUser = sysUserMapper.selectOne(new LambdaQueryWrapper<Users>()
                    .eq(Users::getEmail, registerDTO.getEmail()));
            if (existUser != null) {
                return Result.error("该邮箱已注册");
            }

            // 2.2 校验手机号唯一性（非必填，有值才校验）
            String phone = registerDTO.getPhone();
            if (StringUtils.hasText(phone)) {
                existUser = sysUserMapper.selectOne(new LambdaQueryWrapper<Users>()
                        .eq(Users::getPhone, phone)); // 对应Users的phone字段
                if (existUser != null) {
                    return Result.error("该手机号已注册");
                }
            }

            // 3. 校验验证码
            String redisKey = "verify_code:" + registerDTO.getEmail();
            String redisCode = stringRedisTemplate.opsForValue().get(redisKey);
            if (!StringUtils.hasText(redisCode) || !redisCode.equals(registerDTO.getVerificationCode())) {
                return Result.error("验证码错误或已过期");
            }

            // 4. 处理头像文件（逻辑保留，路径不变）
            String avatarUrl = null;
            if (registerDTO.getAvatar() != null && !registerDTO.getAvatar().isEmpty()) {
                // 4.1 获取src/main/java/edu/cqie/cqdemo/img/userimg的绝对路径
                String basePath = this.getClass().getClassLoader().getResource("").getPath();
                String targetDirPath = basePath.replace("target/classes/", "src/main/java/")
                        + "edu/cqie/cqdemo/img/userimg/";

                // 4.2 创建目录
                File uploadDir = new File(targetDirPath);
                if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                    log.error("创建头像目录失败：{}", targetDirPath);
                    return Result.error("头像目录创建失败");
                }

                // 4.3 生成唯一文件名
                String originalFilename = registerDTO.getAvatar().getOriginalFilename();
                if (originalFilename == null || originalFilename.lastIndexOf(".") == -1) {
                    return Result.error("无效的头像文件（无后缀名）");
                }
                String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFileName = UUID.randomUUID() + suffix;

                // 4.4 保存文件
                File destFile = new File(uploadDir, newFileName);
                registerDTO.getAvatar().transferTo(destFile);

                // 4.5 拼接访问URL
                avatarUrl = "/img/userimg/" + newFileName;
            }

            Users sysUser = new Users();
            sysUser.setUsername(registerDTO.getUsername());
            sysUser.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
            sysUser.setEmail(registerDTO.getEmail());
            sysUser.setGender(registerDTO.getGender());
            sysUser.setPhone(registerDTO.getPhone()); // 手机号映射到phone字段
            sysUser.setAvatarUrl(avatarUrl);
            // 设置创建时间，避免 created_at 为空违反非空约束
            sysUser.setCreatedAt(LocalDateTime.now());

            try {
                sysUser.setExt2(registerDTO.getExt2() != null ? Math.toIntExact(registerDTO.getExt2()) : null);
            } catch (ArithmeticException e) {
                log.error("ext2字段转换失败：{}", registerDTO.getExt2(), e);
                return Result.error("ext2字段数值超出整数范围");
            }

            sysUser.setExt3(registerDTO.getExt3());

            try {
                sysUser.setExt4(registerDTO.getExt4() != null ? LocalDateTime.parse(registerDTO.getExt4()) : null);
            } catch (DateTimeParseException e) {
                log.error("ext4字段时间格式错误：{}", registerDTO.getExt4(), e);
                return Result.error("ext4字段时间格式错误（请使用yyyy-MM-dd'T'HH:mm:ss格式）");
            }

            sysUser.setExt5(registerDTO.getExt5());

            // 6. 保存用户信息
            sysUserMapper.insert(sysUser);

            // 7. 清理验证码
            stringRedisTemplate.delete(redisKey);

            return Result.success("注册成功");
        } catch (IOException e) {
            log.error("头像保存失败：", e);
            return Result.error("头像上传失败（文件IO异常）");
        } catch (Exception e) {
            log.error("注册失败：", e);
            return Result.error("注册失败：" + e.getMessage());
        }
    }
}