package edu.cqie.cqdemo.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 终极兼容版（无弃用警告+无方法解析错误）
 * 1. 保留原有StringRedisTemplate，不修改
 * 2. 构造器直接传入ObjectMapper初始化Jackson2JsonRedisSerializer（官方推荐）
 * 3. 适配所有Spring Data Redis 3.0+版本，兼容AiDTO对象序列化/反序列化
 */
@Configuration
public class RedisConfig {

    // 保留你原有StringRedisTemplate配置，完全不变
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        return template;
    }

    // 核心修改：构造器直接传ObjectMapper，解决所有版本兼容问题
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 1. 配置ObjectMapper（序列化规则不变，开启全字段+类型信息）
        ObjectMapper om = new ObjectMapper();
        // 开启所有字段（包括私有）的序列化
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 开启类型信息，反序列化时能正确识别AiDTO对象（避免转成LinkedHashMap）
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 2. 核心：构造器直接传入配置好的ObjectMapper，无弃用警告+无方法解析问题
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        // 3. String序列化器（处理Redis Key，避免乱码）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 4. 配置序列化规则（Key/HashKey用String，Value/HashValue用JSON）
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jacksonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        // 初始化模板，完成配置
        template.afterPropertiesSet();
        return template;
    }
}