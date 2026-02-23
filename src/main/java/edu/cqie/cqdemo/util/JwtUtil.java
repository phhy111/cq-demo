package edu.cqie.cqdemo.util;

import edu.cqie.cqdemo.entity.LoginUser; // 必须引入自定义的LoginUser
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    // JWT密钥（≥256位，建议配置在yml中，不要用默认值）
    @Value("${jwt.secret:your-secret-key-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef}")
    private String secret;

    // Token过期时间（2小时，单位：毫秒），和Redis保持一致
    @Value("${jwt.expire:7200000}")
    private long expireTime;

    /**
     * 从请求头提取Token（过滤器核心依赖方法）
     * 请求头格式：Authorization: Bearer <token>
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // 去掉"Bearer "前缀
        }
        return null;
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 【新增】从Token中获取用户ID（核心方法，业务层可直接调用）
     * 注意：根据你的Users实体类的id类型调整（Long/Integer/String）
     */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("id", Long.class);
    }

    /**
     * 【新增】从Token中获取用户角色
     */
    public Integer getRoleFromToken(String token) {
        return parseToken(token).get("role", Integer.class);
    }

    /**
     * 验证Token有效性（用户名匹配 + 未过期）
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证Token是否过期
     */
    private boolean isTokenExpired(String token) {
        Date expiration = parseToken(token).getExpiration();
        return expiration.before(new Date());
    }

    /**
     * 解析Token获取Claims（核心内部方法）
     */
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 【核心修复】适配自定义LoginUser，生成含【用户ID+用户名+角色】的Token
     * 入参：UserDetails（实际是LoginUser），内部强转获取ID和用户名
     * 调用：登录接口直接传userDetails即可，无需额外参数
     */
    public String generateToken(UserDetails userDetails) {
        LoginUser loginUser = (LoginUser) userDetails;
        String username = loginUser.getUsername();
        Long userId = loginUser.getId();
        Integer role = loginUser.getRole();

        // 生成JWT Token，写入用户名和ID
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .setSubject(username)        // 标准Claim：存用户名（方便后续验证）
                .claim("id", userId)         // 自定义Claim：存用户ID（核心）
                .claim("role", role)         // 自定义Claim：存用户角色
                .setIssuedAt(new Date())     // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expireTime)) // 过期时间
                .signWith(key)               // 密钥签名
                .compact();
    }

    /**
     * 简化版Token验证（仅校验签名+是否过期）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取Token剩余有效期（毫秒）
     */
    public long getTokenRemainingTime(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * 判断是否需要刷新Token（剩余时间<10分钟）
     */
    public boolean shouldRefreshToken(String token) {
        long remainingTime = getTokenRemainingTime(token);
        return remainingTime > 0 && remainingTime < 10 * 60 * 1000; // 10分钟
    }

    /**
     * 刷新Token（当剩余时间<10分钟时调用）
     */
    public String refreshToken(String token) {
        Claims claims = parseToken(token);
        String username = claims.getSubject();
        Long userId = claims.get("id", Long.class);
        Integer role = claims.get("role", Integer.class);

        // 生成新的Token，保持相同的用户信息
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .setSubject(username)
                .claim("id", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(key)
                .compact();
    }
}