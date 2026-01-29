package edu.cqie.cqdemo.redis.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 封装StringRedisTemplate的常用操作，简化业务层调用
 */
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 设置缓存（带过期时间）
    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    // 获取缓存
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    // 删除缓存
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }
}
