package edu.cqie.cqdemo.ai.preload;

import edu.cqie.cqdemo.ai.template.TravelTemplate;
import edu.cqie.cqdemo.service.AiService;
import edu.cqie.cqdemo.service.impl.AiServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PreloadService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AiServiceFactory aiServiceFactory;

    private static final String PRELOAD_CACHE_KEY_PREFIX = "preload:travel:";
    private static final long PRELOAD_CACHE_EXPIRE = 7;

    private static final List<Map<String, String>> POPULAR_COMBINATIONS = List.of(
            Map.of("days", "3", "type", "亲子", "spots", "洪崖洞,磁器口,解放碑"),
            Map.of("days", "3", "type", "情侣", "spots", "洪崖洞,南山一棵树,解放碑"),
            Map.of("days", "4", "type", "朋友", "spots", "洪崖洞,磁器口,武隆天生三桥"),
            Map.of("days", "2", "type", "穷游", "spots", "洪崖洞,磁器口,解放碑"),
            Map.of("days", "5", "type", "豪华", "spots", "洪崖洞,武隆天生三桥,大足石刻")
    );

    @Scheduled(cron = "0 0 2 * * ?")
    public void preloadPopularTravelPlans() {
        log.info("开始预生成热门旅行计划...");
        
        int successCount = 0;
        int failCount = 0;
        
        for (Map<String, String> combination : POPULAR_COMBINATIONS) {
            try {
                String cacheKey = buildCacheKey(combination);
                
                if (redisTemplate.hasKey(cacheKey)) {
                    log.info("缓存已存在，跳过: {}", cacheKey);
                    continue;
                }
                
                String userMessage = buildUserMessage(combination);
                Long memoryId = 0L;
                
                AiService aiService = aiServiceFactory.createAiService(userMessage);
                
                StringBuilder content = new StringBuilder();
                aiService.generateTravelPlan(memoryId, userMessage)
                        .doOnNext(content::append)
                        .doOnComplete(() -> {
                            if (content.length() > 0) {
                                redisTemplate.opsForValue().set(
                                        cacheKey,
                                        content.toString(),
                                        PRELOAD_CACHE_EXPIRE,
                                        TimeUnit.DAYS
                                );
                                log.info("预生成完成: {}", cacheKey);
                            }
                        })
                        .doOnError(e -> log.error("预生成失败: {}", cacheKey, e))
                        .blockLast();
                
                successCount++;
                
                Thread.sleep(5000);
                
            } catch (Exception e) {
                log.error("预生成失败: {}", combination, e);
                failCount++;
            }
        }
        
        log.info("预生成完成，成功: {}, 失败: {}", successCount, failCount);
    }

    public String getCachedPlan(Map<String, String> params) {
        String cacheKey = buildCacheKey(params);
        return (String) redisTemplate.opsForValue().get(cacheKey);
    }

    public boolean hasCachedPlan(Map<String, String> params) {
        String cacheKey = buildCacheKey(params);
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    }

    public void manualPreload(String days, String type, String spots) {
        Map<String, String> combination = Map.of(
                "days", days,
                "type", type,
                "spots", spots
        );
        
        String cacheKey = buildCacheKey(combination);
        String userMessage = buildUserMessage(combination);
        
        log.info("手动触发预生成: {}", cacheKey);
        
        try {
            AiService aiService = aiServiceFactory.createAiService(userMessage);
            
            StringBuilder content = new StringBuilder();
            aiService.generateTravelPlan(0L, userMessage)
                    .doOnNext(content::append)
                    .doOnComplete(() -> {
                        if (content.length() > 0) {
                            redisTemplate.opsForValue().set(
                                    cacheKey,
                                    content.toString(),
                                    PRELOAD_CACHE_EXPIRE,
                                    TimeUnit.DAYS
                            );
                            log.info("手动预生成完成: {}", cacheKey);
                        }
                    })
                    .doOnError(e -> log.error("手动预生成失败: {}", cacheKey, e))
                    .blockLast();
        } catch (Exception e) {
            log.error("手动预生成失败", e);
        }
    }

    public Map<String, Object> getPreloadStatus() {
        Map<String, Object> status = new HashMap<>();
        List<Map<String, Object>> cacheStatus = new ArrayList<>();
        
        for (Map<String, String> combination : POPULAR_COMBINATIONS) {
            String cacheKey = buildCacheKey(combination);
            boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
            
            Map<String, Object> item = new HashMap<>();
            item.put("combination", combination);
            item.put("cacheKey", cacheKey);
            item.put("cached", exists);
            
            cacheStatus.add(item);
        }
        
        status.put("total", POPULAR_COMBINATIONS.size());
        status.put("cached", cacheStatus.stream().filter(item -> (Boolean) item.get("cached")).count());
        status.put("details", cacheStatus);
        
        return status;
    }

    private String buildCacheKey(Map<String, String> combination) {
        return PRELOAD_CACHE_KEY_PREFIX + combination.get("days") + "d_" + combination.get("type");
    }

    private String buildUserMessage(Map<String, String> combination) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为我生成一份旅行计划，要求如下：");
        sb.append("出行人数：2人，");
        sb.append("旅游天数：").append(combination.get("days")).append("天，");
        sb.append("出行类型：").append(combination.get("type")).append("，");
        sb.append("想去的景点：").append(combination.get("spots"));
        sb.append("content的具体内容Markdown格式的字符串（必须不少于1000字）");
        return sb.toString();
    }
}
