package edu.cqie.cqdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableDiscoveryClient
@MapperScan("edu.cqie.cqdemo.mapper")
@SpringBootApplication(exclude = {
        dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
public class CqDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CqDemoApplication.class, args);
    }
}