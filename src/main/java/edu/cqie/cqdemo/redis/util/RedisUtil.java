package edu.cqie.cqdemo.redis.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * 功能：
 * 1. 基础缓存操作（get/set/delete）
 * 2. 随机抖动TTL（防止缓存雪崩）
 * 3. 逻辑删除（热点key处理）
 * 4. 延迟双删（保证缓存与数据库一致性）
 * 5. 布隆过滤器集成（防止缓存穿透）
 */
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("hotKeyExecutor")
    private Executor hotKeyExecutor;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    private final Random random = new Random();

    // 逻辑删除标记
    private static final String DELETED_MARKER = "__DELETED__";

    // 默认抖动因子 10%
    private static final double DEFAULT_JITTER_FACTOR = 0.1;

    // ==================== 基础操作 ====================

    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public String get(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        // 如果是删除标记，返回null
        if (DELETED_MARKER.equals(value)) {
            return null;
        }
        return value;
    }

    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    // ==================== 随机抖动TTL（防止缓存雪崩）====================

    /**
     * 设置缓存（带随机抖动过期时间）
     * 在基础过期时间上增加随机抖动，避免大量缓存同时过期导致雪崩
     *
     * @param key          缓存key
     * @param value        缓存值
     * @param baseTimeout  基础过期时间
     * @param unit         时间单位
     * @param jitterFactor 抖动因子（如0.1表示±10%的抖动）
     */
    public void setWithJitter(String key, String value, long baseTimeout, TimeUnit unit, double jitterFactor) {
        long jitter = (long) (baseTimeout * jitterFactor);
        long randomJitter = random.nextLong(jitter * 2 + 1) - jitter;
        long finalTimeout = baseTimeout + randomJitter;
        finalTimeout = Math.max(1, finalTimeout);
        stringRedisTemplate.opsForValue().set(key, value, finalTimeout, unit);
    }

    /**
     * 设置缓存（使用默认抖动因子 10%）
     */
    public void setWithJitter(String key, String value, long baseTimeout, TimeUnit unit) {
        setWithJitter(key, value, baseTimeout, unit, DEFAULT_JITTER_FACTOR);
    }

    /**
     * 批量设置缓存（全部带随机抖动，防止雪崩）
     */
    public void batchSetWithJitter(java.util.Map<String, String> keyValueMap, long baseTimeout, TimeUnit unit) {
        for (java.util.Map.Entry<String, String> entry : keyValueMap.entrySet()) {
            setWithJitter(entry.getKey(), entry.getValue(), baseTimeout, unit);
        }
    }

    // ==================== 逻辑删除（热点key处理）====================

    /**
     * 逻辑删除：先标记删除，再异步实际删除
     * 用于热点key，避免直接删除导致大量请求打到数据库
     */
    public void logicalDelete(String key) {
        // 先标记为删除状态，设置一个较短的过期时间（1分钟）
        stringRedisTemplate.opsForValue().set(key, DELETED_MARKER, 1, TimeUnit.MINUTES);
        // 异步实际删除
        hotKeyExecutor.execute(() -> {
            try {
                Thread.sleep(1000);
                stringRedisTemplate.delete(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 逻辑删除 + 异步重建
     * 删除缓存后，异步从数据库重新加载数据到缓存
     */
    public void logicalDeleteAndRebuild(String key, java.util.function.Supplier<String> dbLoader,
                                        long timeout, TimeUnit unit) {
        // 标记删除
        stringRedisTemplate.opsForValue().set(key, DELETED_MARKER, 1, TimeUnit.MINUTES);
        // 异步重建
        hotKeyExecutor.execute(() -> {
            try {
                Thread.sleep(500); // 短暂延迟，让数据库更新完成
                String value = dbLoader.get();
                if (value != null) {
                    setWithJitter(key, value, timeout, unit);
                }
            } catch (Exception e) {
                // 重建失败，记录日志
            }
        });
    }

    // ==================== 延迟双删（保证缓存一致性）====================

    /**
     * 延迟双删策略
     * 步骤：1. 删除缓存 → 2. 更新数据库 → 3. 延迟再次删除缓存
     * 解决并发读写导致的数据不一致问题
     *
     * @param key 缓存key
     */
    public void delayDoubleDelete(String key) {
        // 第一次删除缓存
        stringRedisTemplate.delete(key);
        // 延迟一段时间后再次删除缓存
        hotKeyExecutor.execute(() -> {
            try {
                Thread.sleep(500); // 延迟500ms，确保数据库操作已完成
                stringRedisTemplate.delete(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 延迟双删（带数据库操作）
     * 封装完整的更新流程：删缓存 → 更新DB → 延迟删缓存
     */
    public void updateWithDelayDoubleDelete(String key, Runnable dbUpdater) {
        // 第一次删除缓存
        stringRedisTemplate.delete(key);
        // 更新数据库
        dbUpdater.run();
        // 延迟再次删除缓存
        hotKeyExecutor.execute(() -> {
            try {
                Thread.sleep(500);
                stringRedisTemplate.delete(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // ==================== 布隆过滤器集成（防止缓存穿透）====================

    /**
     * 安全的缓存获取（带布隆过滤器校验）
     * 先查布隆过滤器，如果不存在直接返回null，避免缓存穿透
     */
    public String getWithBloomFilter(String key, String bloomFilterPrefix) {
        // 先查布隆过滤器
        if (!bloomFilterUtil.contains(bloomFilterPrefix, key)) {
            return null; // 布隆过滤器判定不存在，直接返回null
        }
        return get(key);
    }

    /**
     * 安全的缓存设置（同时更新布隆过滤器）
     */
    public void setWithBloomFilter(String key, String value, long timeout, TimeUnit unit, String bloomFilterPrefix) {
        set(key, value, timeout, unit);
        bloomFilterUtil.add(bloomFilterPrefix, key);
    }
}
