package edu.cqie.cqdemo.aspect;

import edu.cqie.cqdemo.annotation.RateLimit;
import edu.cqie.cqdemo.entity.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collections;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LUA_SCRIPT =
            "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local timeout = tonumber(ARGV[2])\n" +
            "local current = tonumber(redis.call('GET', key) or '0')\n" +
            "if current + 1 > limit then\n" +
            "    return 0\n" +
            "else\n" +
            "    current = redis.call('INCR', key)\n" +
            "    if current == 1 then\n" +
            "        redis.call('EXPIRE', key, timeout)\n" +
            "    end\n" +
            "    return 1\n" +
            "end";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(joinPoint, rateLimit);
        int limit = rateLimit.limit();
        int timeout = rateLimit.timeoutSeconds();

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), limit, timeout);

        if (result == null || result == 0) {
            log.warn("请求被限流, key: {}, limit: {}", key, limit);
            throw new RuntimeException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        StringBuilder keyBuilder = new StringBuilder("rate_limit:");
        
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key());
        } else {
            keyBuilder.append(method.getDeclaringClass().getSimpleName())
                      .append(":")
                      .append(method.getName());
        }

        try {
            LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            keyBuilder.append(":user:").append(loginUser.getId());
        } catch (Exception e) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) 
                    RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                keyBuilder.append(":ip:").append(getClientIp(request));
            }
        }

        return keyBuilder.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
