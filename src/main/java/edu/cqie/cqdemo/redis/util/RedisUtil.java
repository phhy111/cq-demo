package edu.cqie.cqdemo.redis.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 封装StringRedisTemplate的常用操作，简化业务层调用
 */
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("hotKeyExecutor")
    private Executor hotKeyExecutor;

    private final Random random = new Random();

    // 逻辑删除标记
    private static final String DELETED_MARKER = "__DELETED__";

    // 设置缓存（带过期时间）
    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    // 设置缓存（带随机抖动过期时间，用于规避缓存雪崩）
    public void setWithJitter(String key, String value, long baseTimeout, TimeUnit unit, double jitterFactor) {
        // 计算抖动范围
        long jitter = (long) (baseTimeout * jitterFactor);
        // 生成随机抖动值（-jitter到+jitter之间）
        long randomJitter = random.nextLong(jitter * 2 + 1) - jitter;
        // 计算最终过期时间
        long finalTimeout = baseTimeout + randomJitter;
        // 确保过期时间大于0
        finalTimeout = Math.max(1, finalTimeout);
        // 设置缓存
        stringRedisTemplate.opsForValue().set(key, value, finalTimeout, unit);
    }

    // 获取缓存
    public String get(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        // 如果是删除标记，返回null
        if (DELETED_MARKER.equals(value)) {
            return null;
        }
        return value;
    }

    // 删除缓存（同步）
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    // 逻辑删除（用于热点key处理）
    public void logicalDelete(String key) {
        // 先标记为删除状态，设置一个较短的过期时间
        stringRedisTemplate.opsForValue().set(key, DELETED_MARKER, 1, TimeUnit.MINUTES);
        // 异步实际删除
        hotKeyExecutor.execute(() -> {
            try {
                // 延迟一段时间再实际删除，避免并发问题
                Thread.sleep(1000);
                stringRedisTemplate.delete(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // 延迟双删策略（用于缓存与数据库一致性）
    public void delayDoubleDelete(String key) {
        // 第一次删除缓存
        stringRedisTemplate.delete(key);
        // 延迟一段时间后再次删除缓存
        hotKeyExecutor.execute(() -> {
            try {
                // 延迟1秒，确保数据库操作已完成
                Thread.sleep(1000);
                stringRedisTemplate.delete(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}

