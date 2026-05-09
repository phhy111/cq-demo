package edu.cqie.cqdemo.ai;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisEmbeddingConfig {

    @Value("${langchain4j.openai.embedding-model.api-key:}")
    private String openaiApiKey;

    @Value("${langchain4j.openai.embedding-model.model-name:text-embedding-3-small}")
    private String embeddingModelName;

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.embedding.index-name:embedding-index}")
    private String embeddingIndexName;

    @Value("${redis.embedding.dimension:1536}")
    private int embeddingDimension;

    @Value("${redis.connection-timeout:30}")
    private int redisConnectionTimeout;

    @Bean
    public EmbeddingModel embeddingModel() {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            log.warn("OpenAI Embedding API Key 未配置，使用模拟嵌入模型");
            return text -> {
                float[] embedding = new float[embeddingDimension];
                for (int i = 0; i < embeddingDimension; i++) {
                    embedding[i] = (float) Math.random() * 2 - 1;
                }
                return new dev.langchain4j.data.embedding.Embedding(embedding);
            };
        }
        
        log.info("初始化 OpenAI Embedding 模型：{}", embeddingModelName);
        return OpenAiEmbeddingModel.builder()
                .apiKey(openaiApiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .build();
    }

    @Bean
    public RedisEmbeddingStore redisEmbeddingStore() {
        log.info("初始化 Redis Embedding Store，主机：{}，端口：{}，索引：{}，维度：{}", 
                redisHost, redisPort, embeddingIndexName, embeddingDimension);
        
        return RedisEmbeddingStore.builder()
                .host(redisHost)
                .port(redisPort)
                .indexName(embeddingIndexName)
                .dimension(embeddingDimension)
                .connectionTimeout(Duration.ofSeconds(redisConnectionTimeout))
                .build();
    }
}
