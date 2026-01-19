package edu.cqie.cqdemo.filter;

import edu.cqie.cqdemo.service.impl.UserDetailsServiceImpl;
import edu.cqie.cqdemo.util.JwtUtil; // 替换全限定名，导入更整洁
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 新增：日志注解
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器：每次请求时验证Token，将用户信息存入SecurityContext
 */
@Component
@RequiredArgsConstructor // 生效条件：依赖的Bean是final且Spring能扫描到
@Slf4j // 新增：日志记录，便于排查问题
public class  JwtAuthenticationFilter extends OncePerRequestFilter {
    // 移除全限定名，改用import导入，代码更整洁
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. 从请求头提取Token
            String token = jwtUtil.getTokenFromRequest(request);
            if (token == null) {
                log.debug("请求头中未获取到JWT Token，放行请求：{}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 从Token中解析用户名（捕获解析异常）
            String username;
            try {
                username = jwtUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                log.error("解析JWT Token失败，请求URI：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 用户名存在且未认证
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("开始校验用户{}的Token，请求URI：{}", username, request.getRequestURI());

                // 4. 加载用户信息（捕获用户不存在异常）
                UserDetails userDetails;
                try {
                    userDetails = userDetailsService.loadUserByUsername(username);
                } catch (Exception e) {
                    log.error("加载用户{}信息失败：{}", username, e.getMessage());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 5. 验证Token有效性
                if (jwtUtil.validateToken(token, userDetails)) {
                    // 6. 设置认证信息到SecurityContext
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.info("用户{}认证成功，已设置到SecurityContext", username);
                } else {
                    log.warn("用户{}的Token无效或已过期，请求URI：{}", username, request.getRequestURI());
                }
            }
        } catch (Exception e) {
            // 捕获所有未预期异常，避免过滤器中断
            log.error("JWT认证过滤器执行异常，请求URI：{}，错误信息：{}", request.getRequestURI(), e.getMessage(), e);
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}