package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.BrowsingHistory;
import java.util.List;

public interface BrowsingHistoryService {
    /**
     * 添加浏览历史
     */
    void addBrowsingHistory(Long userId, Long businessId, Integer type);

    /**
     * 获取用户浏览历史（按类型）
     */
    List<BrowsingHistory> getBrowsingHistoryByType(Long userId, Integer type);

    /**
     * 获取用户所有浏览历史
     */
    List<BrowsingHistory> getAllBrowsingHistory(Long userId);

    /**
     * 删除浏览历史
     */
    void deleteBrowsingHistory(Long userId, Long businessId, Integer type);

    /**
     * 从Redis同步到MySQL
     */
    void syncFromRedisToMysql(Long userId);

    /**
     * 从MySQL加载到Redis
     */
    void loadFromMysqlToRedis(Long userId);
}
