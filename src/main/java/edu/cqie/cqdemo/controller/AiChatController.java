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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String AI_CONVERSATION_KEY_PREFIX = "ai:conversation:";
    private static final long AI_CONVERSATION_EXPIRE = 24L;
    private static final TimeUnit AI_CONVERSATION_TIME_UNIT = TimeUnit.HOURS;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostMapping(
            value = "/history",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> getHistoryByMessage(@RequestBody Map<String, String> request) {
        try {
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();
            String message = request.get("message");
            
            if (message == null) {
                return buildErrorSseResponse(400, "message参数不能为空");
            }
            
            // 构建Redis键的模式，用于匹配该用户的所有对话
            String pattern = AI_CONVERSATION_KEY_PREFIX + userId + ":*";
            Set<String> conversationKeys = redisTemplate.keys(pattern);
            
            Map<String, Object> targetConversation = null;
            if (conversationKeys != null && !conversationKeys.isEmpty()) {
                for (String key : conversationKeys) {
                    Map<String, Object> conversation = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (conversation != null) {
                        String userMessage = (String) conversation.get("userMessage");
                        if (userMessage != null && userMessage.contains(message)) {
                            targetConversation = conversation;
                            break;
                        }
                    }
                }
            }
            
            if (targetConversation == null) {
                return buildErrorSseResponse(404, "未找到对应的历史对话");
            }
            
            String aiMessage = (String) targetConversation.get("aiMessage");
            if (aiMessage == null) {
                return buildErrorSseResponse(500, "历史对话数据不完整");
            }
            
            // 构建SSE响应
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("code", 200);
            doneData.put("msg", "历史对话加载成功");
            doneData.put("data", aiMessage);
            
            ServerSentEvent<String> sseEvent = serializeToSse("ai-done", doneData);
            return Flux.just(sseEvent);
            
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return buildErrorSseResponse(401, e.getMessage());
        } catch (Exception e) {
            log.error("获取历史对话失败", e);
            return buildErrorSseResponse(500, "系统内部错误：" + e.getMessage());
        }
    }

    @GetMapping("/history/list")
    public Map<String, Object> getConversationHistoryList() {
        try {
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();
            
            // 构建Redis键的模式，用于匹配该用户的所有对话
            String pattern = AI_CONVERSATION_KEY_PREFIX + userId + ":*";
            
            // 使用Redis的keys命令获取匹配的所有键
            Set<String> conversationKeys = redisTemplate.keys(pattern);
            
            List<Map<String, Object>> conversationHistory = new ArrayList<>();
            
            if (conversationKeys != null && !conversationKeys.isEmpty()) {
                for (String key : conversationKeys) {
                    Map<String, Object> conversation = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (conversation != null) {
                        conversationHistory.add(conversation);
                    }
                }
                // 按时间戳排序，最新的对话在前面
                conversationHistory.sort((c1, c2) -> {
                    Long t1 = (Long) c1.get("timestamp");
                    Long t2 = (Long) c2.get("timestamp");
                    return t2.compareTo(t1);
                });
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "获取对话历史成功");
            response.put("data", conversationHistory);
            response.put("expireTime", "24小时");
            
            log.info("用户 {} 获取对话历史，共 {} 条记录", userId, conversationHistory.size());
            System.out.println(response);
            return response;
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 401);
            errorResponse.put("msg", e.getMessage());
            return errorResponse;
        } catch (Exception e) {
            log.error("获取对话历史失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("msg", "系统内部错误：" + e.getMessage());
            return errorResponse;
        }
    }

    @DeleteMapping("/history")
    public Map<String, Object> deleteHistoryItem(@RequestParam("timestamp") Long timestamp) {
        try {
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();
            
            if (timestamp == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 400);
                errorResponse.put("msg", "timestamp参数不能为空");
                return errorResponse;
            }
            
            // 构建Redis键的模式，用于匹配该用户的所有对话
            String pattern = AI_CONVERSATION_KEY_PREFIX + userId + ":*";
            Set<String> conversationKeys = redisTemplate.keys(pattern);
            
            boolean deleted = false;
            if (conversationKeys != null && !conversationKeys.isEmpty()) {
                for (String key : conversationKeys) {
                    Map<String, Object> conversation = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (conversation != null) {
                        Long itemTimestamp = (Long) conversation.get("timestamp");
                        if (itemTimestamp != null && itemTimestamp.equals(timestamp)) {
                            // 删除Redis中的记录
                            redisTemplate.delete(key);
                            deleted = true;
                            log.info("用户 {} 删除了对话历史记录，timestamp：{}", userId, timestamp);
                            break;
                        }
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("code", 200);
                response.put("msg", "删除历史对话成功");
            } else {
                response.put("code", 404);
                response.put("msg", "未找到对应的历史对话记录");
            }
            
            return response;
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 401);
            errorResponse.put("msg", e.getMessage());
            return errorResponse;
        } catch (Exception e) {
            log.error("删除历史对话失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("msg", "系统内部错误：" + e.getMessage());
            return errorResponse;
        }
    }

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

            return Flux.defer(() -> {
                        try {
                            AiReport report = aiService.generateTravelPlan(userId, userMessage);
                            String fullJson = OBJECT_MAPPER.writeValueAsString(report);
                            log.debug("结构化AI响应生成成功，JSON长度：{}", fullJson.length());

                            // 存储对话数据到Redis，有效期24小时
                            String conversationKey = AI_CONVERSATION_KEY_PREFIX + userId + ":" + System.currentTimeMillis();
                            Map<String, Object> conversationData = new HashMap<>();
                            conversationData.put("memoryId", userId);
                            conversationData.put("userMessage", userMessage);
                            conversationData.put("aiMessage", fullJson);
                            conversationData.put("timestamp", System.currentTimeMillis());
                            redisTemplate.opsForValue().set(conversationKey, conversationData, AI_CONVERSATION_EXPIRE, AI_CONVERSATION_TIME_UNIT);
                            log.info("对话数据已存储到Redis，键：{}", conversationKey);

                            // 读取Redis中的对话数据（验证存储成功）
                            Map<String, Object> storedConversation = (Map<String, Object>) redisTemplate.opsForValue().get(conversationKey);
                            if (storedConversation != null) {
                                log.info("成功从Redis读取对话数据，memoryId：{}", storedConversation.get("memoryId"));
                            }

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