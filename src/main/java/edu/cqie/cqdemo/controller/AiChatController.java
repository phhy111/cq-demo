package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.dto.AiDTO;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI聊天/旅行计划Controller
 * 已修改：适配【首次全量传参】+【后续仅传userInput】双模式
 * 核心：Redis缓存用户核心旅行数据，基于用户ID判定模式，会话记忆绑定用户
 */
@RestController
@RequestMapping("/api/ai")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AiChatController {

    @Resource
    private AiService aiService;

    // 新增：注入RedisTemplate（用于会话缓存，已配置JSON序列化）
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 常量：Redis缓存key前缀 + 会话过期时间（30分钟，可根据需求调整）
    private static final String AI_SESSION_KEY_PREFIX = "ai:travel:session:";
    private static final long AI_SESSION_EXPIRE = 30L;
    private static final TimeUnit AI_SESSION_TIME_UNIT = TimeUnit.MINUTES;

    /**
     * 核心POST接口【需登录认证（带JWT Token）】
     * 已修改：双模式适配
     * 模式1：首次请求（Redis无缓存）→ 校验全量核心字段 → 拼接全量消息 → 缓存核心数据 → 调用AI
     * 模式2：后续请求（Redis有缓存）→ 仅校验message → 读取缓存核心数据 → 拼接「历史+新要求」→ 调用AI
     */
    @PostMapping(
            value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> chat(@RequestBody(required = true) AiDTO aiDTO) {
        try {
            // 1. 基础校验：请求体非空
            if (aiDTO == null) {
                log.error("AI接口调用失败：请求体不能为空");
                return buildErrorResponse(400, "请求参数不能为空");
            }

            // 2. 优先获取登录用户ID（核心：会话绑定用户，原有逻辑提取到此处，方便后续使用）
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();
            String username = loginUser.getUsername();
            String redisKey = AI_SESSION_KEY_PREFIX + userId; // 每个用户的唯一缓存key
            log.info("AI接口调用：用户ID={}，用户名={}，缓存Key={}", userId, username, redisKey);

            // 3. 从Redis读取用户历史核心数据，判定双模式
            AiDTO historyAiDTO = (AiDTO) redisTemplate.opsForValue().get(redisKey);
            String userMessage; // 最终传给AI服务的拼接消息

            if (historyAiDTO == null) {
                // ===================== 模式1：首次全量请求（无缓存）=====================
                log.info("用户{}为首次请求，进入全量校验模式", userId);
                // 强制校验核心必填字段（原有逻辑保留，无修改）
                if (aiDTO.getPeopleNum() == null || aiDTO.getPeopleNum().isBlank()) {
                    log.error("AI接口调用失败：用户{}出行人数不能为空", userId);
                    return buildErrorResponse(400, "出行人数不能为空");
                }
                if (aiDTO.getPlanDays() == null || aiDTO.getPlanDays().isBlank()) {
                    log.error("AI接口调用失败：用户{}旅游天数不能为空", userId);
                    return buildErrorResponse(400, "旅游天数不能为空");
                }
                if (aiDTO.getTravelType() == null || aiDTO.getTravelType().isBlank()) {
                    log.error("AI接口调用失败：用户{}出行类型不能为空", userId);
                    return buildErrorResponse(400, "出行类型不能为空");
                }

                // 调用原有拼接方法，生成全量消息（原有逻辑保留，无修改）
                userMessage = aiDTO.userMessages();
                // 缓存用户核心数据到Redis（设置过期时间，实现会话过期）
                redisTemplate.opsForValue().set(redisKey, aiDTO, AI_SESSION_EXPIRE, AI_SESSION_TIME_UNIT);
                log.info("用户{}核心数据已缓存到Redis，过期时间{}分钟", userId, AI_SESSION_EXPIRE);

            } else {
                // ===================== 模式2：后续仅输入请求（有缓存）=====================
                log.info("用户{}为后续请求，进入仅输入模式，读取历史缓存数据", userId);
                // 仅校验前端传的message（userInput）非空
                if (aiDTO.getMessage() == null || aiDTO.getMessage().isBlank()) {
                    log.error("AI接口调用失败：用户{}未传入优化要求（message）", userId);
                    return buildErrorResponse(400, "请输入你的旅行优化/新增要求");
                }

                // 调用新增重载方法，拼接「历史核心数据+新message」消息
                userMessage = aiDTO.userMessages(historyAiDTO, aiDTO.getMessage());
                // 刷新Redis缓存过期时间（用户持续操作，会话不过期）
                redisTemplate.expire(redisKey, AI_SESSION_EXPIRE, AI_SESSION_TIME_UNIT);
                log.info("用户{}Redis缓存过期时间已刷新，新要求={}", userId, aiDTO.getMessage());
            }

            // 4. 核心业务：调用Service层（原有逻辑保留，仅传入拼接后的message）
            log.info("AI旅行计划消息拼接完成：{}", userMessage);
            String aiReply = aiService.chat(String.valueOf(userId), userMessage);

            // 5. 封装标准成功响应（原有格式完全保留，前端无需任何修改）
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("username", username);
            data.put("spliceMessage", userMessage);
            data.put("originalParam", aiDTO);
            data.put("aiReply", aiReply);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", historyAiDTO == null ? "旅行计划生成成功" : "旅行计划优化成功");
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (IllegalAccessException e) {
            // 未登录/认证失败（原有逻辑保留，无修改）
            log.error("AI接口调用失败：{}", e.getMessage());
            return buildErrorResponse(401, e.getMessage());
        } catch (Exception e) {
            // 业务异常/未知异常（原有逻辑保留，无修改）
            log.error("AI接口调用异常：", e);
            return buildErrorResponse(500, "AI旅行计划处理失败：" + e.getMessage());
        }
    }

    /**
     * 工具方法：安全获取登录用户信息（原有逻辑，无任何修改）
     */
    private LoginUser getLoginUser() throws IllegalAccessException {
        Object authentication = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (authentication == null) {
            throw new IllegalAccessException("未获取到用户认证信息，请先登录");
        }
        if (!(authentication instanceof LoginUser)) {
            throw new IllegalAccessException("用户认证信息异常，无法获取用户ID");
        }
        return (LoginUser) authentication;
    }

    /**
     * 统一错误响应封装（原有逻辑，无任何修改）
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String msg) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", code);
        errorResponse.put("msg", msg);
        errorResponse.put("data", null);

        HttpStatus httpStatus;
        if (code == 400) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (code == 401) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
}