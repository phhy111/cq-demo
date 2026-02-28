package edu.cqie.cqdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.LoginDTO;
import edu.cqie.cqdemo.dto.RegisterDTO;
import edu.cqie.cqdemo.entity.UserBehaviorLogs;
import edu.cqie.cqdemo.entity.Users;

import edu.cqie.cqdemo.mapper.UserBehaviorLogsMapper;
import edu.cqie.cqdemo.mapper.UserMapper;
import edu.cqie.cqdemo.service.impl.UserDetailsServiceImpl;
import edu.cqie.cqdemo.util.OSSOperationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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
    private final OSSOperationUtil ossOperationUtil;
    private final UserBehaviorLogsMapper userBehaviorLogMapper;


    /**
     * 登录接口（保持不变，登录仍用JSON接收）
     */
    @PostMapping("/login")
    public Result<java.util.Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            // 登录信息写入 Redis，保证 2 小时时效
            String loginKey = "login_token:" + userDetails.getUsername();
            stringRedisTemplate.opsForValue().set(loginKey, token, 30, TimeUnit.MINUTES);

            // 根据用户名查询用户信息
            Users user = sysUserMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Users>()
                    .eq(Users::getUsername, loginDTO.getUsername()));
            System.out.println("当前登录信息"+user);

            // 记录用户行为日志（仅普通用户，role=0）
            if (user.getRole() != null && user.getRole() == 0) {
                UserBehaviorLogs log = new UserBehaviorLogs();
                log.setUserId(user.getId());
                log.setBehaviorType(1); // 1-登录
                log.setBehaviorTime(new java.util.Date());
                // 获取IP地址
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
                // 处理多级代理IP的情况，取第一个IP
                if (ipAddress != null && ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
                log.setIpAddress(ipAddress);
                
                // 获取设备信息
                String userAgent = request.getHeader("User-Agent");
                log.setDeviceInfo(userAgent != null ? userAgent : "Unknown Device");
                
                userBehaviorLogMapper.insert(log);
            }

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

            // 4. 处理头像文件（修改为使用OSS上传到user_avatar/目录）
            String avatarUrl = null;
            if (registerDTO.getAvatar() != null && !registerDTO.getAvatar().isEmpty()) {
                try {
                    // 使用OSS上传头像到user_avatar/目录
                    String imageUrl = ossOperationUtil.upload(registerDTO.getAvatar(), "user_avatar/");
                    avatarUrl = imageUrl;
                } catch (Exception e) {
                    log.error("头像上传失败：", e);
                    return Result.error("头像上传失败：" + e.getMessage());
                }
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

            // 设置个人签名（如果有）
            // sysUser.setPerSignature(registerDTO.getPerSignature());

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
        } catch (Exception e) {
            log.error("注册失败：", e);
            return Result.error("注册失败：" + e.getMessage());
        }
    }
}