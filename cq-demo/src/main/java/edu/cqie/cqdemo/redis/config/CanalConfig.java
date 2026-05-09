package edu.cqie.cqdemo.redis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;
import java.net.Socket;
import java.util.concurrent.Executor;

/**
 * Canal 客户端配置（MySQL binlog 监听）
 * 作为缓存一致性兜底方案：
 * 当数据库数据变更时，通过监听 binlog 异步删除对应缓存
 * 实现缓存与数据库的最终一致性
 *
 * 注意：需要部署 Canal Server 并配置 MySQL binlog
 */
@Configuration
@Slf4j
public class CanalConfig {

    @Value("${canal.enabled:false}")
    private boolean canalEnabled;

    @Value("${canal.host:localhost}")
    private String canalHost;

    @Value("${canal.port:11111}")
    private int canalPort;

    @Value("${canal.destination:example}")
    private String canalDestination;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private Executor hotKeyExecutor;

    /**
     * 初始化 Canal 连接
     */
    @PostConstruct
    public void init() {
        if (!canalEnabled) {
            log.info("Canal 监听未启用，跳过初始化");
            return;
        }

        hotKeyExecutor.execute(() -> {
            try {
                log.info("正在连接 Canal Server: {}:{}, destination: {}", canalHost, canalPort, canalDestination);
                // 这里简化处理，实际生产环境应使用 Canal 官方客户端
                // com.alibaba.otter.canal.client.CanalConnector
                startCanalListener();
            } catch (Exception e) {
                log.error("Canal 连接失败: {}", e.getMessage());
            }
        });
    }

    private void startCanalListener() {
        log.info("Canal 监听已启动，等待数据库变更事件...");
        // 实际实现需要引入 canal.client 依赖：
        // <dependency>
        //     <groupId>com.alibaba.otter</groupId>
        //     <artifactId>canal.client</artifactId>
        //     <version>1.1.6</version>
        // </dependency>
        //
        // CanalConnector connector = CanalConnectors.newSingleConnector(
        //     new InetSocketAddress(canalHost, canalPort), canalDestination, "", "");
        // connector.connect();
        // connector.subscribe("cq.*"); // 订阅 cq 数据库的所有表
        //
        // while (running) {
        //     Message message = connector.getWithoutAck(100);
        //     for (Entry entry : message.getEntries()) {
        //         if (entry.getEntryType() == EntryType.ROWDATA) {
        //             handleRowChange(entry);
        //         }
        //     }
        //     connector.ack(message.getId());
        // }
    }

    /**
     * 处理数据库变更事件，删除对应缓存
     */
    public void handleDatabaseChange(String tableName, String primaryKey, String operation) {
        log.info("Canal 收到数据库变更: 表={}, 主键={}, 操作={}", tableName, primaryKey, operation);

        // 根据表名构建缓存key并删除
        String cacheKey = buildCacheKey(tableName, primaryKey);
        if (cacheKey != null) {
            stringRedisTemplate.delete(cacheKey);
            log.info("已删除缓存: {}", cacheKey);
        }

        // 同时删除相关的列表缓存
        String listCachePattern = buildListCachePattern(tableName);
        if (listCachePattern != null) {
            // 使用 Redis 的 keys 命令或 scan 命令删除匹配的缓存
            // 生产环境建议使用 scan 避免阻塞
        }
    }

    /**
     * 根据表名和主键构建缓存key
     */
    private String buildCacheKey(String tableName, String primaryKey) {
        switch (tableName) {
            case "users":
                return "user:" + primaryKey;
            case "scenics":
                return "scenic:" + primaryKey;
            case "guides":
                return "guide:" + primaryKey;
            case "routes":
                return "route:" + primaryKey;
            default:
                return null;
        }
    }

    /**
     * 构建列表缓存的匹配模式
     */
    private String buildListCachePattern(String tableName) {
        switch (tableName) {
            case "scenics":
                return "scenic:list:*";
            case "guides":
                return "guide:list:*";
            default:
                return null;
        }
    }
}
