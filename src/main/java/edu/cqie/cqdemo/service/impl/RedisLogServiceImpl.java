package edu.cqie.cqdemo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import edu.cqie.cqdemo.entity.RedisLog;
import edu.cqie.cqdemo.service.RedisLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class RedisLogServiceImpl implements RedisLogService {
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    private static final String LOG_KEY_PREFIX = "system:logs:";
    private static final long EXPIRE_DAYS = 200;
    
    @Override
    public void addLog(RedisLog redisLog) {
        try {
            if (redisLog.getLogTime() == null) {
                redisLog.setLogTime(new Date());
            }
            
            String logId = UUID.randomUUID().toString().replace("-", "");
            String logKey = LOG_KEY_PREFIX + "all";
            
            String logJson = JSON.toJSONString(redisLog);
            
            redisTemplate.opsForZSet().add(logKey, logJson, redisLog.getLogTime().getTime());
            
            redisTemplate.expire(logKey, EXPIRE_DAYS, java.util.concurrent.TimeUnit.DAYS);
            
            if (redisLog.getModule() != null) {
                String moduleKey = LOG_KEY_PREFIX + "module:" + redisLog.getModule();
                redisTemplate.opsForZSet().add(moduleKey, logJson, redisLog.getLogTime().getTime());
                redisTemplate.expire(moduleKey, EXPIRE_DAYS, java.util.concurrent.TimeUnit.DAYS);
            }
            
            String idKey = LOG_KEY_PREFIX + "id:" + logId;
            redisTemplate.opsForValue().set(idKey, logJson, EXPIRE_DAYS, java.util.concurrent.TimeUnit.DAYS);
            
            log.debug("日志已保存到 Redis: {}", logId);
        } catch (Exception e) {
            log.error("保存日志到 Redis 失败", e);
        }
    }
    
    @Override
    public Map<String, Object> queryLogs(String module, String logType, Long businessId, Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        List<RedisLog> logList = new ArrayList<>();
        
        try {
            String logKey;
            if (module != null && !module.isEmpty()) {
                logKey = LOG_KEY_PREFIX + "module:" + module;
            } else {
                logKey = LOG_KEY_PREFIX + "all";
            }
            
            if (page == null || page < 1) {
                page = 1;
            }
            if (size == null || size < 1) {
                size = 10;
            }
            
            long start = (page - 1) * size;
            long end = page * size - 1;
            
            Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(logKey, start, end);
            
            if (tuples != null && !tuples.isEmpty()) {
                for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                    try {
                        String json = (String) tuple.getValue();
                        RedisLog redisLog = JSON.parseObject(json, RedisLog.class);
                        
                        if (logType != null && !logType.isEmpty() && !logType.equals(redisLog.getLogType())) {
                            continue;
                        }
                        if (businessId != null && !businessId.equals(redisLog.getBusinessId())) {
                            continue;
                        }
                        
                        logList.add(redisLog);
                    } catch (Exception e) {
                        log.warn("解析日志数据失败", e);
                    }
                }
            }
            
            Long total = redisTemplate.opsForZSet().size(logKey);
            if (total == null) {
                total = 0L;
            }
            
            result.put("list", logList);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            
        } catch (Exception e) {
            log.error("查询日志失败", e);
            result.put("list", new ArrayList<>());
            result.put("total", 0L);
            result.put("page", page);
            result.put("size", size);
        }
        
        return result;
    }
    
    @Override
    public RedisLog getLogById(String logId) {
        try {
            String idKey = LOG_KEY_PREFIX + "id:" + logId;
            String logJson = (String) redisTemplate.opsForValue().get(idKey);
            
            if (logJson != null) {
                return JSON.parseObject(logJson, RedisLog.class);
            }
        } catch (Exception e) {
            log.error("根据 ID 获取日志失败", e);
        }
        return null;
    }
    
    @Override
    public void deleteLog(String logId) {
        try {
            RedisLog redisLog = getLogById(logId);
            if (redisLog != null) {
                String idKey = LOG_KEY_PREFIX + "id:" + logId;
                redisTemplate.delete(idKey);
                
                String logKey = LOG_KEY_PREFIX + "all";
                String logJson = JSON.toJSONString(redisLog);
                redisTemplate.opsForZSet().remove(logKey, logJson);
                
                if (redisLog.getModule() != null) {
                    String moduleKey = LOG_KEY_PREFIX + "module:" + redisLog.getModule();
                    redisTemplate.opsForZSet().remove(moduleKey, logJson);
                }
                
                log.info("日志已删除：{}", logId);
            }
        } catch (Exception e) {
            log.error("删除日志失败", e);
        }
    }
    
    @Override
    public void clearModuleLogs(String module) {
        try {
            String moduleKey = LOG_KEY_PREFIX + "module:" + module;
            redisTemplate.delete(moduleKey);
            log.info("模块日志已清空：{}", module);
        } catch (Exception e) {
            log.error("清空模块日志失败", e);
        }
    }
}
