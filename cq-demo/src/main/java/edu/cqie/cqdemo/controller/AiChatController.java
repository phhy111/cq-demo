package edu.cqie.cqdemo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cqie.cqdemo.ai.preload.PreloadService;
import edu.cqie.cqdemo.ai.template.TravelTemplateService;
import edu.cqie.cqdemo.annotation.RateLimit;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.AiDTO;
import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.security.PromptInjectionFilter;
import edu.cqie.cqdemo.service.AiService;
import edu.cqie.cqdemo.service.GuidesService;
import edu.cqie.cqdemo.service.RoutesService;
import edu.cqie.cqdemo.service.impl.AiServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

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
    private AiServiceFactory aiServiceFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private edu.cqie.cqdemo.redis.util.RedisUtil redisUtil;

    @Autowired
    private PromptInjectionFilter promptInjectionFilter;

    @Autowired
    private TravelTemplateService travelTemplateService;

    @Autowired
    private PreloadService preloadService;

    @Resource
    private RoutesService routesService;

    @Resource
    private GuidesService guidesService;

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
    @RateLimit(limit = 30, timeoutSeconds = 60, key = "ai-history")
    public Flux<String> getHistoryByMessage(@RequestBody Map<String, String> request) {
        try {
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();
            String travelWay = request.get("travelWay");
            String message = request.get("message");

            if (message == null) {
                return Flux.error(new IllegalArgumentException("message参数不能为空"));
            }
            if (travelWay == null) {
                return Flux.error(new IllegalArgumentException("travelWay参数不能为空"));
            }

            PromptInjectionFilter.ValidationResult validation = promptInjectionFilter.validate(message);
            if (!validation.isValid()) {
                return Flux.error(new IllegalArgumentException(validation.getMessage()));
            }

            String pattern = AI_CONVERSATION_KEY_PREFIX + userId + ":*";
            Set<String> conversationKeys = redisTemplate.keys(pattern);

            Map<String, Object> targetConversation = null;
            if (conversationKeys != null && !conversationKeys.isEmpty()) {
                for (String key : conversationKeys) {
                    Map<String, Object> conversation = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (conversation != null) {
                        String userMessage = (String) conversation.get("userMessage");
                        String storedTravelWay = (String) conversation.get("travelWay");
                        if (userMessage != null && userMessage.contains(message) &&
                                (storedTravelWay == null || storedTravelWay.equals(travelWay))) {
                            targetConversation = conversation;
                            break;
                        }
                    }
                }
            }

            if (targetConversation == null) {
                return Flux.error(new RuntimeException("未找到对应的历史对话"));
            }

            String aiMessage = (String) targetConversation.get("aiMessage");
            if (aiMessage == null) {
                return Flux.error(new RuntimeException("历史对话数据不完整"));
            }

            return Flux.just(aiMessage);

        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return Flux.error(e);
        } catch (Exception e) {
            log.error("获取历史对话失败", e);
            return Flux.error(e);
        }
    }

    @GetMapping("/history/list")
    @RateLimit(limit = 30, timeoutSeconds = 60, key = "ai-history-list")
    public Map<String, Object> getConversationHistoryList() {
        try {
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();

            String pattern = AI_CONVERSATION_KEY_PREFIX + userId + ":*";
            Set<String> conversationKeys = redisTemplate.keys(pattern);

            List<Map<String, Object>> conversationHistory = new ArrayList<>();

            if (conversationKeys != null && !conversationKeys.isEmpty()) {
                for (String key : conversationKeys) {
                    Map<String, Object> conversation = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (conversation != null) {
                        conversationHistory.add(conversation);
                    }
                }
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

    @PostMapping("/addChatMessage")
    @Transactional
    @RateLimit(limit = 20, timeoutSeconds = 60, key = "ai-add-message")
    public Result<Map<String, Object>> addChatMessage(@RequestBody Map<String, Object> request) {
        try {
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();

            Map<String, Object> aiDTOMap = (Map<String, Object>) request.get("aiDTO");
            String markdownContent = (String) request.get("markdownContent");

            log.info("前端发送的aiDTOMap: {}", aiDTOMap);
            log.info("前端发送的markdownContent长度: {}", markdownContent != null ? markdownContent.length() : 0);

            if (aiDTOMap == null || markdownContent == null || markdownContent.isEmpty()) {
                return Result.error("请求参数不能为空");
            }

            Routes routes = new Routes();
            routes.setUserId(userId);
            routes.setTitle((String) aiDTOMap.get("title"));
            routes.setDescription((String) aiDTOMap.get("description"));
            routes.setSubtitle((String) aiDTOMap.get("subtitle"));
            routesService.save(routes);

            Guides guides = new Guides();
            guides.setUserId(userId);
            guides.setTitle((String) aiDTOMap.get("title"));
            guides.setSummary((String) aiDTOMap.get("summary"));
            guides.setContent(markdownContent);
            guides.setCategory((String) aiDTOMap.get("travelType"));

            Object moneyObj = aiDTOMap.get("budget");
            if (moneyObj != null) {
                if (moneyObj instanceof Integer) {
                    guides.setBudgetInfo(((Integer) moneyObj).doubleValue());
                } else if (moneyObj instanceof Double) {
                    guides.setBudgetInfo((Double) moneyObj);
                } else if (moneyObj instanceof String) {
                    try {
                        guides.setBudgetInfo(Double.parseDouble((String) moneyObj));
                    } catch (NumberFormatException e) {
                        log.warn("预算格式错误: {}", moneyObj);
                    }
                }
            }

            String travelWay = (String) aiDTOMap.get("travelWay");
            guides.setTravelWay(travelWay);
            guides.setRoutesId(routes.getId());

            guidesService.save(guides);

            log.info("用户 {} 保存了对话消息，Routes ID: {}, Guides ID: {}", userId, routes.getId(), guides.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("routesId", routes.getId());
            result.put("guidesId", guides.getId());
            result.put("msg", "保存成功");
            return Result.success(result);
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return Result.error("用户未登录或令牌无效");
        } catch (Exception e) {
            log.error("保存对话消息失败", e);
            return Result.error("系统内部错误：" + e.getMessage());
        }
    }

    @DeleteMapping("/history")
    @RateLimit(limit = 20, timeoutSeconds = 60, key = "ai-delete-history")
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

            String pattern = AI_CONVERSATION_KEY_PREFIX + userId + ":*";
            Set<String> conversationKeys = redisTemplate.keys(pattern);

            boolean deleted = false;
            if (conversationKeys != null && !conversationKeys.isEmpty()) {
                for (String key : conversationKeys) {
                    Map<String, Object> conversation = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (conversation != null) {
                        Long itemTimestamp = (Long) conversation.get("timestamp");
                        if (itemTimestamp != null && itemTimestamp.equals(timestamp)) {
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

    @DeleteMapping("/deleteChatMessage")
    @Transactional(rollbackFor = Exception.class)
    @RateLimit(limit = 20, timeoutSeconds = 60, key = "ai-delete-message")
    public Result<String> deleteChatMessage(@RequestParam("routesId") String routesIdStr) {
        try {
            LoginUser loginUser = this.getLoginUser();
            Long userId = loginUser.getId();

            Integer routesId;
            try {
                Long routesIdLong = Long.parseLong(routesIdStr.trim());
                if (routesIdLong > Integer.MAX_VALUE || routesIdLong < Integer.MIN_VALUE) {
                    return Result.error("routesId参数超出整数范围");
                }
                routesId = routesIdLong.intValue();
            } catch (NumberFormatException e) {
                return Result.error("routesId参数必须是有效的数字");
            }

            Routes routes = routesService.getById(routesId);
            if (routes == null) {
                return Result.error("未找到对应的路线信息");
            }
            if (!routes.getUserId().equals(userId)) {
                return Result.error("无权删除其他用户的路线信息");
            }

            QueryWrapper<Guides> guidesQueryWrapper = new QueryWrapper<>();
            guidesQueryWrapper.eq("routes_id", routesId);
            boolean guidesDeleted = guidesService.remove(guidesQueryWrapper);

            if (!guidesDeleted) {
                log.warn("删除Guides失败，Routes ID: {}", routesId);
            }

            boolean routesDeleted = routesService.removeById(routesId);

            if (routesDeleted) {
                log.info("用户 {} 删除了对话消息，Routes ID: {}", userId, routesId);
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败，请稍后重试");
            }
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return Result.error("用户未登录或令牌无效");
        } catch (Exception e) {
            log.error("删除对话消息失败", e);
            return Result.error("系统内部错误：" + e.getMessage());
        }
    }

    @PostMapping(
            value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @RateLimit(limit = 10, timeoutSeconds = 60, key = "ai-chat", message = "AI对话请求过于频繁，请稍后再试")
    public Flux<String> chat(@RequestBody(required = true) AiDTO aiDTO) {
        if (aiDTO == null) {
            return Flux.just("data:" + toJson(Result.error("请求参数不能为空")) + "\n\n");
        }

        LoginUser loginUser;
        try {
            loginUser = this.getLoginUser();
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return Flux.just("data:" + toJson(Result.error("用户未登录或令牌无效")) + "\n\n");
        }

        Long userId = loginUser.getId();
        String redisKey = AI_SESSION_KEY_PREFIX + userId;

        String userMessage;
        AiDTO historyAiDTO = (AiDTO) redisTemplate.opsForValue().get(redisKey);

        if (historyAiDTO == null) {
            if (aiDTO.getPeopleNum() == null || aiDTO.getPeopleNum().isBlank()) {
                return Flux.just("data:" + toJson(Result.error("出行人数不能为空")) + "\n\n");
            }
            if (aiDTO.getPlanDays() == null || aiDTO.getPlanDays().isBlank()) {
                return Flux.just("data:" + toJson(Result.error("旅游天数不能为空")) + "\n\n");
            }
            if (aiDTO.getTravelType() == null || aiDTO.getTravelType().isBlank()) {
                return Flux.just("data:" + toJson(Result.error("出行类型不能为空")) + "\n\n");
            }
            
            String baseMessage = aiDTO.userMessages()
                    + "content的具体内容Markdown格式的字符串（必须不少于1000字），"
                    + "如果时间足够景点可以挑选一些其他符合的，不用局限于所给的几个想去景点，但是注意顺路";
            
            if (aiDTO.getTemplateCode() != null && !aiDTO.getTemplateCode().isBlank()) {
                userMessage = travelTemplateService.buildMessageWithTemplate(aiDTO.getTemplateCode(), baseMessage);
                log.info("使用攻略模板: {}", aiDTO.getTemplateCode());
            } else {
                userMessage = baseMessage;
            }
            
            redisTemplate.opsForValue().set(redisKey, aiDTO, AI_SESSION_EXPIRE, AI_SESSION_TIME_UNIT);
        } else {
            if (aiDTO.getMessage() == null || aiDTO.getMessage().isBlank()) {
                return Flux.just("data:" + toJson(Result.error("请输入你的旅行优化/新增要求")) + "\n\n");
            }

            PromptInjectionFilter.ValidationResult validation = promptInjectionFilter.validate(aiDTO.getMessage());
            if (!validation.isValid()) {
                return Flux.just("data:" + toJson(Result.error(validation.getMessage())) + "\n\n");
            }

            userMessage = aiDTO.userMessages(historyAiDTO, aiDTO.getMessage())
                    + "content的具体内容Markdown格式的字符串（必须不少于1000字），"
                    + "如果时间足够景点可以挑选一些其他符合的，不用局限于所给的几个想去景点，但是注意顺路";
            redisUtil.expireWithJitter(redisKey, AI_SESSION_EXPIRE, AI_SESSION_TIME_UNIT);
        }

        log.info("AI消息拼接完成，用户ID: {}, 消息长度: {}", userId, userMessage.length());

        final Long finalUserId = userId;
        final AiDTO finalAiDTO = aiDTO;
        final String finalUserMessage = userMessage;

        StringBuilder fullContent = new StringBuilder();

        AiService routedAiService = aiServiceFactory.createAiService(finalUserMessage);
        log.info("AI对话使用动态路由模型，用户消息长度：{}", finalUserMessage.length());

        Flux<String> stream = routedAiService.generateTravelPlan(finalUserId, finalUserMessage);

        return stream
                .doOnNext(fullContent::append)
                .doOnComplete(() -> {
                    try {
                        String markdown = fullContent.toString();
                        if (markdown.isEmpty()) {
                            log.warn("AI生成内容为空，跳过Redis存储");
                            return;
                        }

                        String conversationKey = AI_CONVERSATION_KEY_PREFIX + finalUserId
                                + ":" + System.currentTimeMillis();
                        Map<String, Object> conversationData = new HashMap<>();
                        conversationData.put("memoryId", finalUserId);
                        conversationData.put("userMessage", finalUserMessage);
                        conversationData.put("aiMessage", markdown);
                        conversationData.put("travelWay", finalAiDTO.getTravelWay());
                        conversationData.put("travelType", finalAiDTO.getTravelType());
                        conversationData.put("templateCode", finalAiDTO.getTemplateCode());
                        conversationData.put("timestamp", System.currentTimeMillis());

                        redisTemplate.opsForValue().set(
                                conversationKey,
                                conversationData,
                                AI_CONVERSATION_EXPIRE,
                                AI_CONVERSATION_TIME_UNIT
                        );
                        log.info("对话数据已存储到Redis，键：{}", conversationKey);
                    } catch (Exception e) {
                        log.error("存储对话数据到Redis失败", e);
                    }
                })
                .doOnError(e -> log.error("AI流式生成异常，userId={}", finalUserId, e));
    }

    private String toJson(Result<?> result) {
        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "{\"code\":500,\"msg\":\"系统内部错误\"}";
        }
    }

    private LoginUser getLoginUser() throws IllegalAccessException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser)) {
            throw new IllegalAccessException("用户未登录或令牌无效");
        }
        return (LoginUser) principal;
    }
}
