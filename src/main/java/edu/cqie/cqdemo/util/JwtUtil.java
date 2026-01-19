package edu.cqie.cqdemo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * 完善后的JWT工具类（包含过滤器所需的所有方法）
 */
@Component
public class JwtUtil {
    // JWT密钥（≥256位，建议配置在yml中）
    @Value("${jwt.secret:your-secret-key-1234567890abcdef1234567890abcdef}")
    private String secret;

    // Token过期时间（2小时，单位：毫秒）
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
     * 验证Token有效性（适配过滤器的重载方法）
     * @param token JWT Token
     * @param userDetails 用户信息
     * @return true=有效，false=无效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            // 验证用户名匹配 + Token未过期
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证Token是否过期（内部方法）
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
     * 生成Token（登录接口用）
     */
    public String generateToken(UserDetails userDetails) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .setSubject(userDetails.getUsername()) // 用户名
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expireTime)) // 过期时间
                .signWith(key) // 签名
                .compact();
    }

    /**
     * 简化版Token验证（仅校验是否过期/签名正确）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}