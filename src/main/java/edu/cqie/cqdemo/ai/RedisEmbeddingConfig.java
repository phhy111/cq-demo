package edu.cqie.cqdemo.ai;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisEmbeddingConfig {

    @Bean
    public RedisEmbeddingStore redisEmbeddingStore() {
        return RedisEmbeddingStore.builder()
                .host("localhost")
                .port(6379)
                .indexName("embedding-index")
                .dimension(1536)
                .build();
    }
}
