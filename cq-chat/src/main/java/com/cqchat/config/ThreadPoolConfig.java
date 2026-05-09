package com.cqchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    /**
     * Netty 业务处理线程池
     * 用于将数据库/Redis 等阻塞操作从 Netty IO 线程移出
     */
    @Bean("businessExecutor")
    public ExecutorService businessExecutor() {
        return new ThreadPoolExecutor(
                4,
                8,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
