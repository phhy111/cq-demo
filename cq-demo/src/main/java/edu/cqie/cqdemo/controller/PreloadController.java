package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.ai.preload.PreloadService;
import edu.cqie.cqdemo.annotation.RateLimit;
import edu.cqie.cqdemo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/preload")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PreloadController {

    @Autowired
    private PreloadService preloadService;

    @GetMapping("/status")
    @RateLimit(limit = 30, timeoutSeconds = 60, key = "preload-status")
    public Result<Map<String, Object>> getPreloadStatus() {
        try {
            Map<String, Object> status = preloadService.getPreloadStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取预生成状态失败", e);
            return Result.error("获取预生成状态失败：" + e.getMessage());
        }
    }

    @PostMapping("/trigger")
    @RateLimit(limit = 5, timeoutSeconds = 300, key = "preload-trigger", message = "预生成请求过于频繁，请稍后再试")
    public Result<String> triggerPreload(
            @RequestParam String days,
            @RequestParam String type,
            @RequestParam String spots) {
        try {
            if (days == null || days.isBlank() || type == null || type.isBlank() || spots == null || spots.isBlank()) {
                return Result.error("参数不能为空");
            }
            
            preloadService.manualPreload(days, type, spots);
            return Result.success("预生成任务已提交，请稍后查看状态");
        } catch (Exception e) {
            log.error("触发预生成失败", e);
            return Result.error("触发预生成失败：" + e.getMessage());
        }
    }

    @GetMapping("/check")
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "preload-check")
    public Result<Map<String, Object>> checkCache(
            @RequestParam String days,
            @RequestParam String type,
            @RequestParam String spots) {
        try {
            Map<String, String> params = Map.of(
                    "days", days,
                    "type", type,
                    "spots", spots
            );
            
            boolean exists = preloadService.hasCachedPlan(params);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("cached", exists);
            
            if (exists) {
                result.put("plan", preloadService.getCachedPlan(params));
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查缓存失败", e);
            return Result.error("检查缓存失败：" + e.getMessage());
        }
    }
}
