package edu.cqie.cqdemo.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AsyncTaskService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TASK_QUEUE_KEY = "async:task:queue";
    private static final String TASK_RESULT_PREFIX = "async:task:result:";
    private static final long TASK_RESULT_EXPIRE = 24;

    @Async("taskExecutor")
    public CompletableFuture<String> executeAsyncTask(String taskId, String taskType, Map<String, Object> params) {
        log.info("开始执行异步任务: {}, 类型: {}", taskId, taskType);
        
        try {
            updateTaskStatus(taskId, "PROCESSING", null);
            
            String result = processTask(taskType, params);
            
            updateTaskStatus(taskId, "COMPLETED", result);
            
            log.info("异步任务完成: {}", taskId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("异步任务失败: {}", taskId, e);
            updateTaskStatus(taskId, "FAILED", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public String submitTask(String taskType, Map<String, Object> params) {
        String taskId = generateTaskId();
        
        Map<String, Object> taskInfo = Map.of(
                "taskId", taskId,
                "taskType", taskType,
                "params", params,
                "status", "PENDING",
                "createdAt", System.currentTimeMillis()
        );
        
        redisTemplate.opsForValue().set(
                TASK_RESULT_PREFIX + taskId,
                taskInfo,
                TASK_RESULT_EXPIRE,
                TimeUnit.HOURS
        );
        
        redisTemplate.opsForList().rightPush(TASK_QUEUE_KEY, taskId);
        
        log.info("提交异步任务: {}, 类型: {}", taskId, taskType);
        return taskId;
    }

    public Map<String, Object> getTaskStatus(String taskId) {
        return (Map<String, Object>) redisTemplate.opsForValue().get(TASK_RESULT_PREFIX + taskId);
    }

    private void updateTaskStatus(String taskId, String status, String result) {
        Map<String, Object> taskInfo = (Map<String, Object>) redisTemplate.opsForValue().get(TASK_RESULT_PREFIX + taskId);
        if (taskInfo != null) {
            taskInfo.put("status", status);
            taskInfo.put("result", result);
            taskInfo.put("updatedAt", System.currentTimeMillis());
            
            redisTemplate.opsForValue().set(
                    TASK_RESULT_PREFIX + taskId,
                    taskInfo,
                    TASK_RESULT_EXPIRE,
                    TimeUnit.HOURS
            );
        }
    }

    private String processTask(String taskType, Map<String, Object> params) throws Exception {
        switch (taskType) {
            case "TRAVEL_PLAN":
                return processTravelPlanTask(params);
            case "WEATHER_ANALYSIS":
                return processWeatherAnalysisTask(params);
            case "ROUTE_OPTIMIZATION":
                return processRouteOptimizationTask(params);
            default:
                throw new IllegalArgumentException("未知的任务类型: " + taskType);
        }
    }

    private String processTravelPlanTask(Map<String, Object> params) {
        log.info("处理旅行计划任务: {}", params);
        return "旅行计划生成完成";
    }

    private String processWeatherAnalysisTask(Map<String, Object> params) {
        log.info("处理天气分析任务: {}", params);
        return "天气分析完成";
    }

    private String processRouteOptimizationTask(Map<String, Object> params) {
        log.info("处理路线优化任务: {}", params);
        return "路线优化完成";
    }

    private String generateTaskId() {
        return "task_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
    }
}
