package edu.cqie.cqdemo.service.impl;

import edu.cqie.cqdemo.entity.BrowsingHistory;
import edu.cqie.cqdemo.mapper.BrowsingHistoryMapper;
import edu.cqie.cqdemo.redis.util.RedisUtil;
import edu.cqie.cqdemo.service.BrowsingHistoryService;
import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BrowsingHistoryServiceImpl implements BrowsingHistoryService {

    @Autowired
    private BrowsingHistoryMapper browsingHistoryMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 29天过期时间
    private static final long EXPIRE_DAYS = 29;
    // 同步锁
    private final ReentrantLock syncLock = new ReentrantLock();

    // Redis键前缀
    private String getHistoryKey(Long userId) {
        return "browsing_history:" + userId;
    }

    private String getHistoryTypeKey(Long userId, Integer type) {
        return "browsing_history:" + userId + ":" + type;
    }

    @Override
    public void addBrowsingHistory(Long userId, Long businessId, Integer type) {
        // 1. 先添加到Redis
        String key = getHistoryKey(userId);
        String typeKey = getHistoryTypeKey(userId, type);

        BrowsingHistory history = new BrowsingHistory();
        history.setUserId(userId);
        history.setBusinessId(businessId);
        history.setType(type);
        Date now = new Date();
        history.setCreateTime(now);
        history.setUpdateTime(now);

        // 从Redis获取现有历史
        String existingHistory = redisUtil.get(key);
        List<BrowsingHistory> historyList;
        if (existingHistory != null) {
            historyList = JSON.parseArray(existingHistory, BrowsingHistory.class);
        } else {
            // 如果Redis中没有，尝试从MySQL加载
            if (syncLock.tryLock()) {
                try {
                    // 双重检查
                    existingHistory = redisUtil.get(key);
                    if (existingHistory == null) {
                        loadFromMysqlToRedis(userId);
                        existingHistory = redisUtil.get(key);
                        historyList = existingHistory != null ?
                                JSON.parseArray(existingHistory, BrowsingHistory.class) : new ArrayList<>();
                    } else {
                        historyList = JSON.parseArray(existingHistory, BrowsingHistory.class);
                    }
                } finally {
                    syncLock.unlock();
                }
            } else {
                // 锁被占用，直接创建新列表
                historyList = new ArrayList<>();
            }
        }

        // 添加新历史记录（去重）
        historyList.removeIf(h -> h.getBusinessId().equals(businessId) && h.getType().equals(type));
        historyList.add(0, history); // 最新的在前面

        // 保存回Redis
        redisUtil.set(key, JSON.toJSONString(historyList), EXPIRE_DAYS, TimeUnit.DAYS);
        redisUtil.set(typeKey, JSON.toJSONString(historyList.stream()
                .filter(h -> h.getType().equals(type))
                .toList()), EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Override
    public List<BrowsingHistory> getBrowsingHistoryByType(Long userId, Integer type) {
        String key = getHistoryTypeKey(userId, type);
        String historyJson = redisUtil.get(key);

        if (historyJson != null) {
            return JSON.parseArray(historyJson, BrowsingHistory.class);
        } else {
            // Redis中没有，从MySQL加载
            if (syncLock.tryLock()) {
                try {
                    // 双重检查
                    historyJson = redisUtil.get(key);
                    if (historyJson == null) {
                        loadFromMysqlToRedis(userId);
                        historyJson = redisUtil.get(key);
                    }
                } finally {
                    syncLock.unlock();
                }
            }

            if (historyJson != null) {
                return JSON.parseArray(historyJson, BrowsingHistory.class);
            } else {
                // 从MySQL直接查询
                return browsingHistoryMapper.selectByUserIdAndType(userId, type);
            }
        }
    }

    @Override
    public List<BrowsingHistory> getAllBrowsingHistory(Long userId) {
        String key = getHistoryKey(userId);
        String historyJson = redisUtil.get(key);

        if (historyJson != null) {
            return JSON.parseArray(historyJson, BrowsingHistory.class);
        } else {
            // Redis中没有，从MySQL加载
            if (syncLock.tryLock()) {
                try {
                    // 双重检查
                    historyJson = redisUtil.get(key);
                    if (historyJson == null) {
                        loadFromMysqlToRedis(userId);
                        historyJson = redisUtil.get(key);
                    }
                } finally {
                    syncLock.unlock();
                }
            }

            if (historyJson != null) {
                return JSON.parseArray(historyJson, BrowsingHistory.class);
            } else {
                // 从MySQL直接查询
                return browsingHistoryMapper.selectByUserId(userId);
            }
        }
    }

    @Override
    public void deleteBrowsingHistory(Long userId, Long businessId, Integer type) {
        // 1. 从Redis删除
        String key = getHistoryKey(userId);
        String typeKey = getHistoryTypeKey(userId, type);

        String existingHistory = redisUtil.get(key);
        if (existingHistory != null) {
            List<BrowsingHistory> historyList = JSON.parseArray(existingHistory, BrowsingHistory.class);
            historyList.removeIf(h -> h.getBusinessId().equals(businessId) && h.getType().equals(type));
            redisUtil.set(key, JSON.toJSONString(historyList), EXPIRE_DAYS, TimeUnit.DAYS);
        }

        String existingTypeHistory = redisUtil.get(typeKey);
        if (existingTypeHistory != null) {
            List<BrowsingHistory> typeHistoryList = JSON.parseArray(existingTypeHistory, BrowsingHistory.class);
            typeHistoryList.removeIf(h -> h.getBusinessId().equals(businessId) && h.getType().equals(type));
            redisUtil.set(typeKey, JSON.toJSONString(typeHistoryList), EXPIRE_DAYS, TimeUnit.DAYS);
        }

        // 2. 从MySQL删除
        browsingHistoryMapper.deleteByUserIdAndBusinessId(userId, businessId, type);
    }

    @Override
    @Transactional
    public void syncFromRedisToMysql(Long userId) {
        if (syncLock.tryLock()) {
            try {
                String key = getHistoryKey(userId);
                String historyJson = redisUtil.get(key);

                if (historyJson != null) {
                    List<BrowsingHistory> historyList = JSON.parseArray(historyJson, BrowsingHistory.class);

                    // 先删除用户的所有历史记录
                    browsingHistoryMapper.deleteByUserId(userId);

                    // 重新插入
                    for (BrowsingHistory history : historyList) {
                        // 确保updateTime不为null
                        if (history.getUpdateTime() == null) {
                            Date now = new Date();
                            if (history.getCreateTime() == null) {
                                history.setCreateTime(now);
                            }
                            history.setUpdateTime(now);
                        }
                        browsingHistoryMapper.insert(history);
                    }
                }
            } finally {
                syncLock.unlock();
            }
        }
    }

    @Override
    public void loadFromMysqlToRedis(Long userId) {
        List<BrowsingHistory> historyList = browsingHistoryMapper.selectByUserId(userId);
        if (!historyList.isEmpty()) {
            String key = getHistoryKey(userId);
            redisUtil.set(key, JSON.toJSONString(historyList), EXPIRE_DAYS, TimeUnit.DAYS);

            // 按类型缓存
            for (Integer type : new int[]{1, 2, 3, 4, 6}) {
                List<BrowsingHistory> typeHistory = historyList.stream()
                        .filter(h -> h.getType().equals(type))
                        .toList();
                if (!typeHistory.isEmpty()) {
                    String typeKey = getHistoryTypeKey(userId, type);
                    redisUtil.set(typeKey, JSON.toJSONString(typeHistory), EXPIRE_DAYS, TimeUnit.DAYS);
                }
            }
        }
    }
}
