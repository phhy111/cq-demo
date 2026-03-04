package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.RedisLog;
import edu.cqie.cqdemo.service.RedisLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@Slf4j
public class RedisLogController {
    
    @Autowired
    private RedisLogService redisLogService;
    
    /**
     * 添加日志
     */
    @PostMapping("/add")
    public Result addLog(@RequestBody RedisLog redisLog) {
        try {
            if (redisLog.getLogTime() == null) {
                redisLog.setLogTime(new Date());
            }
            redisLogService.addLog(redisLog);
            return Result.success("日志添加成功");
        } catch (Exception e) {
            log.error("添加日志失败", e);
            return Result.error("日志添加失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询日志列表
     */
    @GetMapping("/query")
    public Result queryLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) Long businessId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            Map<String, Object> result = redisLogService.queryLogs(module, logType, businessId, page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询日志失败", e);
            return Result.error("查询日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据 ID 获取日志详情
     */
    @GetMapping("/detail/{logId}")
    public Result getLogById(@PathVariable String logId) {
        try {
            RedisLog redisLog = redisLogService.getLogById(logId);
            if (redisLog != null) {
                return Result.success(redisLog);
            } else {
                return Result.error("日志不存在");
            }
        } catch (Exception e) {
            log.error("获取日志详情失败", e);
            return Result.error("获取日志详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除日志
     */
    @DeleteMapping("/delete/{logId}")
    public Result deleteLog(@PathVariable String logId) {
        try {
            redisLogService.deleteLog(logId);
            return Result.success("日志删除成功");
        } catch (Exception e) {
            log.error("删除日志失败", e);
            return Result.error("删除日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 清空指定模块的日志
     */
    @DeleteMapping("/clear/{module}")
    public Result clearModuleLogs(@PathVariable String module) {
        try {
            redisLogService.clearModuleLogs(module);
            return Result.success("模块日志清空成功");
        } catch (Exception e) {
            log.error("清空模块日志失败", e);
            return Result.error("清空模块日志失败：" + e.getMessage());
        }
    }
}
