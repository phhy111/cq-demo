package edu.cqie.cqdemo.util;

import edu.cqie.cqdemo.entity.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret:your-secret-key-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef}")
    private String secret;

    // Access Token 过期时间：30分钟（短有效期，保证安全）
    @Value("${jwt.access-token-expire:1800000}")
    private long accessTokenExpireTime;

    // Refresh Token 过期时间：7天（长有效期，用于续期）
    @Value("${jwt.refresh-token-expire:604800000}")
    private long refreshTokenExpireTime;

    /**
     * 从请求头提取 Access Token
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 从请求头提取 Refresh Token
     */
    public String getRefreshTokenFromRequest(HttpServletRequest request) {
        String refreshHeader = request.getHeader("X-Refresh-Token");
        if (refreshHeader != null && refreshHeader.startsWith("Bearer ")) {
            return refreshHeader.substring(7);
        }
        return null;
    }

    /**
     * 生成双 Token（Access Token + Refresh Token）
     */
    public Map<String, String> generateTokenPair(UserDetails userDetails) {
        LoginUser loginUser = (LoginUser) userDetails;
        String username = loginUser.getUsername();
        Long userId = loginUser.getId();
        Integer role = loginUser.getRole();

        String jti = UUID.randomUUID().toString();

        String accessToken = buildToken(username, userId, role, jti, accessTokenExpireTime, "access");
        String refreshToken = buildToken(username, userId, role, jti, refreshTokenExpireTime, "refresh");

        Map<String, String> tokenPair = new HashMap<>();
        tokenPair.put("accessToken", accessToken);
        tokenPair.put("refreshToken", refreshToken);
        tokenPair.put("jti", jti);
        return tokenPair;
    }

    /**
     * 使用 Refresh Token 生成新的 Access Token
     */
    public String refreshAccessToken(String refreshToken) {
        Claims claims = parseToken(refreshToken);
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new RuntimeException("非法的 Refresh Token");
        }

        String username = claims.getSubject();
        Long userId = claims.get("id", Long.class);
        Integer role = claims.get("role", Integer.class);
        String jti = claims.get("jti", String.class);

        return buildToken(username, userId, role, jti, accessTokenExpireTime, "access");
    }

    /**
     * 构建 Token
     */
    private String buildToken(String username, Long userId, Integer role, String jti, long expireTime, String type) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .setSubject(username)
                .claim("id", userId)
                .claim("role", role)
                .claim("jti", jti)
                .claim("type", type)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("id", Long.class);
    }

    public Integer getRoleFromToken(String token) {
        return parseToken(token).get("role", Integer.class);
    }

    public String getJtiFromToken(String token) {
        return parseToken(token).get("jti", String.class);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = parseToken(token).getExpiration();
        return expiration.before(new Date());
    }

    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getTokenRemainingTime(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(parseToken(token).get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
