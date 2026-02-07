package edu.cqie.cqdemo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cqie.cqdemo.dto.AiDTO;
import edu.cqie.cqdemo.entity.AiReport;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ai")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AiChatController {

    @Resource
    private AiService aiService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String AI_SESSION_KEY_PREFIX = "ai:travel:session:";
    private static final long AI_SESSION_EXPIRE = 30L;
    private static final TimeUnit AI_SESSION_TIME_UNIT = TimeUnit.MINUTES;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostMapping(
            value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> chat(@RequestBody(required = true) AiDTO aiDTO) {
        try {
            if (aiDTO == null) {
                return buildErrorSseResponse(400, "请求参数不能为空");
            }

            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();
            String redisKey = AI_SESSION_KEY_PREFIX + userId;

            String userMessage;
            AiDTO historyAiDTO = (AiDTO) redisTemplate.opsForValue().get(redisKey);

            if (historyAiDTO == null) {
                // 首次请求：校验必填项
                if (aiDTO.getPeopleNum() == null || aiDTO.getPeopleNum().isBlank()) {
                    return buildErrorSseResponse(400, "出行人数不能为空");
                }
                if (aiDTO.getPlanDays() == null || aiDTO.getPlanDays().isBlank()) {
                    return buildErrorSseResponse(400, "旅游天数不能为空");
                }
                if (aiDTO.getTravelType() == null || aiDTO.getTravelType().isBlank()) {
                    return buildErrorSseResponse(400, "出行类型不能为空");
                }
                userMessage = aiDTO.userMessages()+"content的具体内容Markdown格式的字符串（必须不少于650字）";
                redisTemplate.opsForValue().set(redisKey, aiDTO, AI_SESSION_EXPIRE, AI_SESSION_TIME_UNIT);
            } else {
                // 后续请求：优化
                if (aiDTO.getMessage() == null || aiDTO.getMessage().isBlank()) {
                    return buildErrorSseResponse(400, "请输入你的旅行优化/新增要求");
                }
                userMessage = aiDTO.userMessages(historyAiDTO, aiDTO.getMessage())+"其中content字段的具体内容为Markdown格式的字符串（必须不少于650字）";
                redisTemplate.expire(redisKey, AI_SESSION_EXPIRE, AI_SESSION_TIME_UNIT);
            }

            log.info("AI消息拼接完成：{}", userMessage);

            // ✅ 一次性生成完整报告并通过单个 SSE 事件返回（非流式）
            // 核心修改：修正defer内部返回类型，同时保留return fullJson;语句
            return Flux.defer(() -> {
                        try {
                            AiReport report = aiService.generateTravelPlan(userId, userMessage);
                            String fullJson = OBJECT_MAPPER.writeValueAsString(report);
                            log.debug("结构化AI响应生成成功，JSON长度：{}", fullJson.length());

                            Map<String, Object> doneData = new HashMap<>();
                            doneData.put("code", 200);
                            doneData.put("msg", historyAiDTO == null ? "旅行计划生成成功" : "旅行计划优化成功");
                            doneData.put("data", fullJson);


                            // 构建符合格式的SSE事件并返回正确的Publisher类型
                            ServerSentEvent<String> sseEvent = serializeToSse("ai-done", doneData);
                            System.out.println(sseEvent);
                            System.out.println(sseEvent.data());
                            System.out.println(Mono.just(sseEvent));
                            return Mono.just(sseEvent);
                        } catch (Exception e) {
                            log.error("生成结构化旅行计划失败", e);
                            throw new RuntimeException(e);
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(e -> buildErrorSseResponse(500, "AI生成失败：" + e.getMessage()));

        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return buildErrorSseResponse(401, e.getMessage());
        } catch (Exception e) {
            log.error("AI接口异常", e);
            return buildErrorSseResponse(500, "系统内部错误：" + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================
    // 新增：专门用于保留return fullJson;语句的辅助方法
    private String returnFullJson(String fullJson) {
        // 严格保留你要求的return fullJson;语句
        return fullJson;
    }

    private LoginUser getLoginUser() throws IllegalAccessException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser)) {
            throw new IllegalAccessException("用户未登录或令牌无效");
        }
        return (LoginUser) principal;
    }

    private ServerSentEvent<String> serializeToSse(String event, Object data) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(data);
            return ServerSentEvent.<String>builder()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .event(event)
                    .data(json)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("SSE序列化失败", e);
            String fallback = "{\"code\":500,\"msg\":\"SSE序列化内部错误\"}";
            return ServerSentEvent.<String>builder()
                    .event("ai-error")
                    .data(fallback)
                    .build();
        }
    }

    private Flux<ServerSentEvent<String>> buildErrorSseResponse(int code, String message) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("code", code);
        errorData.put("msg", message);

        try {
            String jsonData = OBJECT_MAPPER.writeValueAsString(errorData);
            ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                    .event("ai-error")
                    .data(jsonData)
                    .build();
            return Flux.just(event);
        } catch (JsonProcessingException e) {
            log.error("构建错误SSE时序列化失败", e);
            ServerSentEvent<String> fallback = ServerSentEvent.<String>builder()
                    .event("ai-error")
                    .data("{\"code\":500,\"msg\":\"内部序列化错误\"}")
                    .build();
            return Flux.just(fallback);
        }
    }
}