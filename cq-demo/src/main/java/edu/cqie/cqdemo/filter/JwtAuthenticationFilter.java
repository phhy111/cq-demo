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
 * JWT认证过滤器：双Token机制（Access Token + Refresh Token）
 * Access Token：30分钟有效期，用于接口认证
 * Refresh Token：7天有效期，用于无感知续期Access Token
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
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String accessToken = jwtUtil.getTokenFromRequest(request);

            // 1. 没有Token，直接放行，交由SecurityConfig处理
            if (accessToken == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 验证Access Token
            String username;
            try {
                username = jwtUtil.getUsernameFromToken(accessToken);
            } catch (Exception e) {
                log.error("解析JWT Token失败，请求URI：{}，错误信息：{}", request.getRequestURI(), e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"Token解析失败\"}");
                return;
            }

            // 3. 用户名存在且未认证
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("开始校验用户{}的Token，请求URI：{}", username, request.getRequestURI());

                UserDetails userDetails;
                try {
                    userDetails = userDetailsService.loadUserByUsername(username);
                } catch (Exception e) {
                    log.error("加载用户{}信息失败：{}", username, e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"msg\":\"用户不存在\"}");
                    return;
                }

                // 4. 验证Access Token有效性
                if (jwtUtil.validateToken(accessToken, userDetails)) {
                    // Access Token有效，设置认证信息
                    setAuthentication(request, userDetails);
                    filterChain.doFilter(request, response);
                } else {
                    // Access Token过期或无效，尝试用Refresh Token续期
                    handleTokenRefresh(request, response, filterChain, userDetails);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            log.error("JWT认证过滤器执行异常，请求URI：{}，错误信息：{}", request.getRequestURI(), e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"msg\":\"服务器认证异常\"}");
        }
    }

    /**
     * 处理Token刷新：Access Token过期时，使用Refresh Token无感知续期
     */
    private void handleTokenRefresh(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain, UserDetails userDetails) throws IOException, ServletException {
        String refreshToken = jwtUtil.getRefreshTokenFromRequest(request);

        if (refreshToken == null) {
            log.warn("Access Token已过期，且未提供Refresh Token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"msg\":\"Token已过期，请重新登录\"}");
            return;
        }

        try {
            // 验证Refresh Token有效性
            if (jwtUtil.validateToken(refreshToken, userDetails)) {
                // 生成新的Access Token
                String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
                // 将新Access Token通过响应头返回给前端
                response.setHeader("Authorization", "Bearer " + newAccessToken);
                response.setHeader("X-Token-Refreshed", "true");
                log.info("用户{}的Access Token已过期，通过Refresh Token无感知续期成功", userDetails.getUsername());

                // 设置认证信息并放行
                setAuthentication(request, userDetails);
                filterChain.doFilter(request, response);
            } else {
                log.warn("Refresh Token已过期或无效，用户{}需要重新登录", userDetails.getUsername());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"msg\":\"登录已过期，请重新登录\"}");
            }
        } catch (Exception e) {
            log.error("Refresh Token验证失败：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"msg\":\"Token刷新失败，请重新登录\"}");
        }
    }

    /**
     * 设置认证信息到SecurityContext
     */
    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.info("用户{}认证成功，已设置到SecurityContext", userDetails.getUsername());
    }
}
