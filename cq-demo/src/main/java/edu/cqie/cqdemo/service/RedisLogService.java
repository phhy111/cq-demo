package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.RedisLog;

import java.util.List;
import java.util.Map;

public interface RedisLogService {
    
    /**
     * 添加日志到 Redis
     * @param redisLog 日志对象
     */
    void addLog(RedisLog redisLog);
    
    /**
     * 根据条件查询日志
     * @param module 模块
     * @param logType 日志类型
     * @param businessId 业务 ID
     * @param page 页码
     * @param size 每页大小
     * @return 日志列表
     */
    Map<String, Object> queryLogs(String module, String logType, Long businessId, Integer page, Integer size);
    
    /**
     * 根据 ID 获取日志详情
     * @param logId 日志 ID
     * @return 日志详情
     */
    RedisLog getLogById(String logId);
    
    /**
     * 删除日志
     * @param logId 日志 ID
     */
    void deleteLog(String logId);
    
    /**
     * 清空指定模块的日志
     * @param module 模块
     */
    void clearModuleLogs(String module);
}
