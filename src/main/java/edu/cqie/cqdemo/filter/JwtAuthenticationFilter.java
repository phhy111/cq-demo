package edu.cqie.cqdemo.filter;

import edu.cqie.cqdemo.service.impl.UserDetailsServiceImpl;
import edu.cqie.cqdemo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 对OPTIONS预检请求直接放行（解决CORS跨域问题）
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // 对无需登录的接口直接放行
            String uri = request.getRequestURI();
            if (uri.startsWith("/api/auth/login")
                    || uri.startsWith("/api/auth/register")
                    || uri.startsWith("/api/auth/sendCode")
                    || uri.startsWith("/api/comments/AddCommentsInfo")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 1. 从请求头提取Token
            String token = jwtUtil.getTokenFromRequest(request);
            if (token == null) {
                log.debug("请求头中未获取到JWT Token，拒绝访问：{}", request.getRequestURI());
                // 无Token时直接返回401，终止请求
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"未登录，请先登录\"}");
                return; // 关键：终止过滤器链，不再放行
            }

            // 2. 从Token中解析用户名
            String username;
            try {
                username = jwtUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                log.error("解析JWT Token失败，请求URI：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
                // Token解析失败，返回401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"Token解析失败\"}");
                return;
            }

            // 3. 用户名存在且未认证
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("开始校验用户{}的Token，请求URI：{}", username, request.getRequestURI());

                // 4. 加载用户信息
                UserDetails userDetails;
                try {
                    userDetails = userDetailsService.loadUserByUsername(username);
                } catch (Exception e) {
                    log.error("加载用户{}信息失败：{}", username, e.getMessage());
                    // 用户不存在，返回401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"msg\":\"用户不存在\"}");
                    return;
                }

                // 5. 验证Token有效性
                if (jwtUtil.validateToken(token, userDetails)) {
                    // 6. 检查是否需要刷新Token（剩余时间<10分钟）
                    if (jwtUtil.shouldRefreshToken(token)) {
                        // 生成新的Token
                        String newToken = jwtUtil.refreshToken(token);
                        // 将新Token通过响应头返回给前端
                        response.setHeader("Authorization", "Bearer " + newToken);
                        log.info("用户{}的Token剩余时间不足10分钟，已自动刷新", username);
                    }
                    
                    // 7. 设置认证信息到SecurityContext
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.info("用户{}认证成功，已设置到SecurityContext", username);
                    // Token有效，放行请求
                    filterChain.doFilter(request, response);
                } else {
                    log.warn("用户{}的Token无效或已过期，请求URI：{}", username, request.getRequestURI());
                    // Token无效/过期，返回401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"msg\":\"Token已过期或无效\"}");
                    return;
                }
            } else {
                // 用户名空或已认证，正常放行
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            log.error("JWT认证过滤器执行异常，请求URI：{}，错误信息：{}", request.getRequestURI(), e.getMessage(), e);
            // 未知异常，返回500
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"msg\":\"服务器认证异常\"}");
        }
    }
}