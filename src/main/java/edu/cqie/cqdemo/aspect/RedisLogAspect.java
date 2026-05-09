package edu.cqie.cqdemo.aspect;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.entity.RedisLog;
import edu.cqie.cqdemo.service.RedisLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Date;

@Aspect
@Component
@Slf4j
public class RedisLogAspect {
    
    @Autowired
    private RedisLogService redisLogService;
    
    @Pointcut("@annotation(edu.cqie.cqdemo.annotation.RedisLog)")
    public void logPointcut() {
    }
    
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        
        String requestURL = request.getRequestURI();
        String requestMethod = request.getMethod();
        String requestParams = safeToJson(point.getArgs());
        String ipAddress = getIpAddress(request);
        
        RedisLog redisLog = RedisLog.builder()
                .requestUrl(requestURL)
                .requestMethod(requestMethod)
                .requestParams(requestParams)
                .ipAddress(ipAddress)
                .logTime(new Date())
                .build();
        
        setOperatorInfo(redisLog);
        
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        if (method.isAnnotationPresent(edu.cqie.cqdemo.annotation.RedisLog.class)) {
            edu.cqie.cqdemo.annotation.RedisLog logAnnotation = method.getAnnotation(
                    edu.cqie.cqdemo.annotation.RedisLog.class);
            redisLog.setModule(logAnnotation.module());
            redisLog.setLogType(logAnnotation.type());
            redisLog.setBusinessId(getBusinessId(point.getArgs()));
        }
        
        Object result = null;
        try {
            result = point.proceed();
            redisLog.setResponseData(safeToJson(result));
            redisLog.setExecuteTime(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            redisLog.setLogType("ERROR");
            redisLog.setResponseData("{\"error\": \"" + escapeJson(e.getMessage()) + "\"}");
            redisLog.setExecuteTime(System.currentTimeMillis() - startTime);
            throw e;
        }
        
        redisLogService.addLog(redisLog);
        
        return result;
    }
    
    private void setOperatorInfo(RedisLog redisLog) {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof LoginUser) {
                LoginUser loginUser = (LoginUser) principal;
                redisLog.setOperatorId(loginUser.getId());
                redisLog.setOperatorName(loginUser.getUsername());
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败，可能是未登录请求");
        }
    }
    
    private Long getBusinessId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        
        for (Object arg : args) {
            if (arg instanceof Integer) {
                return Long.valueOf((Integer) arg);
            } else if (arg instanceof Long) {
                return (Long) arg;
            } else if (arg instanceof String) {
                try {
                    return Long.parseLong((String) arg);
                } catch (NumberFormatException e) {
                }
            }
        }
        return null;
    }
    
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * 安全的 JSON 序列化，处理循环引用和特殊类型
     */
    private String safeToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JSON.toJSONString(obj, 
                    SerializerFeature.WriteMapNullValue,
                    SerializerFeature.WriteDateUseDateFormat,
                    SerializerFeature.DisableCircularReferenceDetect);
        } catch (Exception e) {
            log.warn("JSON 序列化失败，使用 toString: {}", e.getMessage());
            return "{\"_toString\": \"" + escapeJson(obj.toString()) + "\"}";
        }
    }
    
    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
