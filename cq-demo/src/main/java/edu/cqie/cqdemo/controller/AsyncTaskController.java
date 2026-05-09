package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.async.AsyncTaskService;
import edu.cqie.cqdemo.annotation.RateLimit;
import edu.cqie.cqdemo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/async")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AsyncTaskController {

    @Autowired
    private AsyncTaskService asyncTaskService;

    @PostMapping("/submit")
    @RateLimit(limit = 10, timeoutSeconds = 60, key = "async-submit")
    public Result<Map<String, String>> submitTask(
            @RequestParam String taskType,
            @RequestBody Map<String, Object> params) {
        try {
            if (taskType == null || taskType.isBlank()) {
                return Result.error("任务类型不能为空");
            }
            
            String taskId = asyncTaskService.submitTask(taskType, params);
            
            Map<String, String> result = new java.util.HashMap<>();
            result.put("taskId", taskId);
            result.put("status", "PENDING");
            result.put("message", "任务已提交，请稍后查询结果");
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("提交任务失败", e);
            return Result.error("提交任务失败：" + e.getMessage());
        }
    }

    @GetMapping("/status/{taskId}")
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "async-status")
    public Result<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        try {
            if (taskId == null || taskId.isBlank()) {
                return Result.error("任务ID不能为空");
            }
            
            Map<String, Object> taskStatus = asyncTaskService.getTaskStatus(taskId);
            if (taskStatus == null) {
                return Result.error("未找到该任务");
            }
            
            return Result.success(taskStatus);
        } catch (Exception e) {
            log.error("查询任务状态失败", e);
            return Result.error("查询任务状态失败：" + e.getMessage());
        }
    }
}
