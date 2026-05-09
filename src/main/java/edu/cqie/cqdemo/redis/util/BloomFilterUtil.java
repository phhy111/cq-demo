package edu.cqie.cqdemo.redis.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 布隆过滤器工具类，用于防止缓存穿透
 * 使用Redis的BitMap实现
 */
@Component
public class BloomFilterUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 布隆过滤器的key前缀
    private static final String BLOOM_FILTER_PREFIX = "bloom:filter:";
    
    // 布隆过滤器的大小（位数）
    private static final int BLOOM_FILTER_SIZE = 1000000;
    
    // 哈希函数数量
    private static final int HASH_FUNCTION_COUNT = 3;

    /**
     * 向布隆过滤器中添加元素
     * @param keyPrefix 业务key前缀
     * @param value 要添加的值
     */
    public void add(String keyPrefix, String value) {
        String bloomKey = BLOOM_FILTER_PREFIX + keyPrefix;
        for (int i = 0; i < HASH_FUNCTION_COUNT; i++) {
            int hash = getHash(value, i);
            stringRedisTemplate.opsForValue().setBit(bloomKey, hash, true);
        }
    }

    /**
     * 判断元素是否可能在布隆过滤器中
     * @param keyPrefix 业务key前缀
     * @param value 要判断的值
     * @return true：可能存在，false：一定不存在
     */
    public boolean contains(String keyPrefix, String value) {
        String bloomKey = BLOOM_FILTER_PREFIX + keyPrefix;
        for (int i = 0; i < HASH_FUNCTION_COUNT; i++) {
            int hash = getHash(value, i);
            if (!stringRedisTemplate.opsForValue().getBit(bloomKey, hash)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算哈希值
     * @param value 要计算哈希的值
     * @param seed 哈希种子
     * @return 哈希值
     */
    private int getHash(String value, int seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest((value + seed).getBytes());
            int hash = 0;
            for (byte b : bytes) {
                hash = hash * 31 + (b & 0xFF);
            }
            return Math.abs(hash) % BLOOM_FILTER_SIZE;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
