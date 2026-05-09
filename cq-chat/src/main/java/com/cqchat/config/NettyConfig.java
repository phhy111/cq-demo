package com.cqchat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "netty")
public class NettyConfig {
    private int port = 9090;
    private int bossThreads = 1;
    private int workerThreads = 4;
    private int heartbeatIdle = 60;
}
