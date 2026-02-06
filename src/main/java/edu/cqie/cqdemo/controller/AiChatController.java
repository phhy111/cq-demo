package edu.cqie.cqdemo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cqie.cqdemo.dto.AiDTO;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI聊天/旅行计划Controller
 * 最终修复版：
 * 1. SSE的data字段强制序列化为纯JSON字符串（核心解决解析报错）
 * 2. 统一返回Flux<ServerSentEvent<String>>，前端可直接解析
 * 3. 双模式逻辑闭环，错误响应标准化
 */
@RestController
@RequestMapping("/api/ai")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AiChatController {

    @Resource
    private AiService aiService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 常量：Redis缓存key前缀 + 会话过期时间（30分钟）
    private static final String AI_SESSION_KEY_PREFIX = "ai:travel:session:";
    private static final long AI_SESSION_EXPIRE = 30L;
    private static final TimeUnit AI_SESSION_TIME_UNIT = TimeUnit.MINUTES;
    // JSON序列化工具（全局复用）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 核心POST接口【需登录认证（带JWT Token）】
     * 关键修改：返回值改为Flux<ServerSentEvent<String>>，确保data是纯JSON字符串
     */
    @PostMapping(
            value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE // SSE标准媒体类型
    )
    public Flux<ServerSentEvent<String>> chat(@RequestBody(required = true) AiDTO aiDTO) {
        // 外层try-catch包裹，统一处理所有异常并返回流式错误
        try {
            // 1. 基础校验：请求体非空
            if (aiDTO == null) {
                log.error("AI接口调用失败：请求体不能为空");
                return buildErrorSseResponse(400, "请求参数不能为空");
            }

            // 2. 获取登录用户ID（核心：会话绑定用户）
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();
            String username = loginUser.getUsername();
            String redisKey = AI_SESSION_KEY_PREFIX + userId;
            log.info("AI接口调用：用户ID={}，用户名={}，缓存Key={}", userId, username, redisKey);

            // 3. 声明userMessage为方法级变量，解决作用域问题
            String userMessage;
            // 从Redis读取历史数据，判定双模式
            AiDTO historyAiDTO = (AiDTO) redisTemplate.opsForValue().get(redisKey);

            if (historyAiDTO == null) {
                // ===================== 模式1：首次全量请求（无缓存）=====================
                log.info("用户{}为首次请求，进入全量校验模式", userId);
                // 强制校验核心必填字段
                if (aiDTO.getPeopleNum() == null || aiDTO.getPeopleNum().isBlank()) {
                    log.error("AI接口调用失败：用户{}出行人数不能为空", userId);
                    return buildErrorSseResponse(400, "出行人数不能为空");
                }
                if (aiDTO.getPlanDays() == null || aiDTO.getPlanDays().isBlank()) {
                    log.error("AI接口调用失败：用户{}旅游天数不能为空", userId);
                    return buildErrorSseResponse(400, "旅游天数不能为空");
                }
                if (aiDTO.getTravelType() == null || aiDTO.getTravelType().isBlank()) {
                    log.error("AI接口调用失败：用户{}出行类型不能为空", userId);
                    return buildErrorSseResponse(400, "出行类型不能为空");
                }

                // 拼接全量消息
                userMessage = aiDTO.userMessages();
                // 缓存核心数据到Redis（设置过期时间）
                redisTemplate.opsForValue().set(redisKey, aiDTO, AI_SESSION_EXPIRE, AI_SESSION_TIME_UNIT);
                log.info("用户{}核心数据已缓存到Redis，过期时间{}分钟", userId, AI_SESSION_EXPIRE);

            } else {
                // ===================== 模式2：后续仅输入请求（有缓存）=====================
                log.info("用户{}为后续请求，进入仅输入模式，读取历史缓存数据", userId);
                // 仅校验前端传的message（userInput）非空
                if (aiDTO.getMessage() == null || aiDTO.getMessage().isBlank()) {
                    log.error("AI接口调用失败：用户{}未传入优化要求（message）", userId);
                    return buildErrorSseResponse(400, "请输入你的旅行优化/新增要求");
                }

                // 拼接「历史核心数据+新message」消息
                userMessage = aiDTO.userMessages(historyAiDTO, aiDTO.getMessage());
                // 刷新Redis缓存过期时间
                redisTemplate.expire(redisKey, AI_SESSION_EXPIRE, AI_SESSION_TIME_UNIT);
                log.info("用户{}Redis缓存过期时间已刷新，新要求={}", userId, aiDTO.getMessage());
            }

            // 4. 核心业务：调用流式AI服务（统一模式1/2为流式响应）
            log.info("AI旅行计划消息拼接完成：{}", userMessage);
            // 调用chatStream获取流式响应，并封装为带元数据的SSE
            return aiService.chatStream(userId, userMessage)
                    .map(chunk -> {
                        // 封装单条流式数据（AI实时返回的内容）
                        Map<String, Object> data = new HashMap<>();
                        data.put("userId", userId);
                        data.put("username", username);
                        data.put("spliceMessage", userMessage);
                        data.put("originalParam", aiDTO);

                        // ===================== 核心修复 START =====================
                        // 问题：AI返回的chunk是非JSON文本（如id:1770373, content:xxx）
                        // 修复：将非JSON文本解析为标准JSON对象，避免前端解析报错
                        Map<String, String> aiReplyMap = new HashMap<>();
                        if (chunk != null && !chunk.isBlank()) {
                            // 分割AI原始响应（兼容"id:xxx, content:xxx"格式）
                            String[] parts = chunk.split(",");
                            for (String part : parts) {
                                String[] keyValue = part.split(":", 2); // 只分割第一个冒号，避免内容含冒号
                                if (keyValue.length == 2) {
                                    String key = keyValue[0].trim();
                                    String value = keyValue[1].trim();
                                    aiReplyMap.put(key, value);
                                }
                            }
                            // 兜底：如果未解析出内容，直接存入原始文本到content字段
                            if (aiReplyMap.isEmpty()) {
                                aiReplyMap.put("content", chunk);
                            }
                        } else {
                            aiReplyMap.put("content", ""); // 空内容兜底
                        }
                        data.put("aiReplyChunk", aiReplyMap); // 现在是标准JSON对象
                        // ===================== 核心修复 END =====================

                        data.put("code", 200);
                        data.put("msg", historyAiDTO == null ? "旅行计划生成成功" : "旅行计划优化成功");

                        // 关键修改：手动序列化为JSON字符串，确保data是纯JSON
                        String jsonData;
                        try {
                            jsonData = OBJECT_MAPPER.writeValueAsString(data);
                        } catch (JsonProcessingException e) {
                            log.error("JSON序列化失败", e);
                            jsonData = "{\"code\":500,\"msg\":\"数据序列化失败\",\"data\":null}";
                        }

                        // 构建标准SSE响应（data为纯JSON字符串）
                        return ServerSentEvent.<String>builder()
                                .id(String.valueOf(System.currentTimeMillis())) // SSE唯一ID
                                .event("ai-chat") // SSE事件类型（前端可监听）
                                .data(jsonData)
                                .build();
                    })
                    .onErrorResume(e -> {
                        // 流式响应过程中异常处理
                        log.error("AI流式响应异常：", e);
                        return buildErrorSseResponse(500, "AI响应异常：" + e.getMessage());
                    });

        } catch (IllegalAccessException e) {
            // 未登录/认证失败异常处理
            log.error("AI接口调用失败：{}", e.getMessage());
            return buildErrorSseResponse(401, e.getMessage());
        } catch (Exception e) {
            // 通用业务异常处理
            log.error("AI接口调用异常：", e);
            return buildErrorSseResponse(500, "AI旅行计划处理失败：" + e.getMessage());
        }
    }

    /**
     * 工具方法：安全获取登录用户信息（原有逻辑，无修改）
     */
    private LoginUser getLoginUser() throws IllegalAccessException {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalAccessException("未获取到用户认证信息，请先登录");
        }

        Object principal = auth.getPrincipal();
        if (principal == null) {
            throw new IllegalAccessException("用户认证主体信息为空，无法获取用户ID");
        }

        log.info("获取到的认证主体Principal类型：{}，实际值：{}", principal.getClass().getName(), principal);

        if (!(principal instanceof LoginUser)) {
            throw new IllegalAccessException(
                    "用户认证信息异常，无法获取用户ID！当前Principal类型为：" + principal.getClass().getName()
                            + "，预期类型为：" + LoginUser.class.getName()
            );
        }

        return (LoginUser) principal;
    }

    /**
     * 修正版：构建流式SSE错误响应（返回String类型，data为纯JSON字符串）
     */
    private Flux<ServerSentEvent<String>> buildErrorSseResponse(int code, String msg) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("code", code);
        errorData.put("msg", msg);
        errorData.put("data", null);

        // 手动序列化为JSON字符串
        String jsonData;
        try {
            jsonData = OBJECT_MAPPER.writeValueAsString(errorData);
        } catch (JsonProcessingException e) {
            log.error("错误响应序列化失败", e);
            jsonData = "{\"code\":500,\"msg\":\"错误序列化失败\",\"data\":null}";
        }

        // 构建标准SSE错误响应
        ServerSentEvent<String> errorEvent = ServerSentEvent.<String>builder()
                .id(String.valueOf(System.currentTimeMillis()))
                .event("ai-error") // 错误事件类型（前端可区分）
                .data(jsonData)
                .build();

        // 返回包含单个错误事件的Flux
        return Flux.just(errorEvent);
    }
}